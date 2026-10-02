package com.gescal.service;

import org.sqlite.SQLiteConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DatabaseConfigurationService {
    private static final String KEY = "gescal.database.path";
    private static final Set<String> REQUIRED_TABLES = Set.of(
            "adresses", "clients", "operations", "tournees", "reglements", "distribution");

    private final Path activePath;
    private final AtomicReference<Path> configuredPath;
    private final Path configPath;

    public DatabaseConfigurationService(@Value("${gescal.database.path}") String databasePath,
                                        @Value("${gescal.config.path:./config/gescal.properties}") String configPath) {
        this.activePath = normalize(databasePath);
        this.configuredPath = new AtomicReference<>(this.activePath);
        this.configPath = Path.of(configPath).toAbsolutePath().normalize();
    }

    public Path activePath() { return activePath; }
    public Path configuredPath() { return configuredPath.get(); }
    public boolean restartRequired() { return !activePath.equals(configuredPath.get()); }

    public Optional<Path> chooseAndPersist() throws Exception {
        Optional<Path> selected = chooseFile(configuredPath());
        if (selected.isPresent()) setAndPersist(selected.get());
        return selected;
    }

    public synchronized void setAndPersist(Path selected) throws Exception {
        Path validated = validateDatabase(selected);
        persist(validated);
        configuredPath.set(validated);
    }

    Path validateDatabase(Path selected) throws Exception {
        Path candidate = selected.toAbsolutePath().normalize();
        if (!Files.isRegularFile(candidate)) throw new IllegalArgumentException("Le fichier sélectionné n'existe pas.");
        if (!Files.isReadable(candidate)) throw new IllegalArgumentException("Le fichier sélectionné n'est pas lisible.");

        SQLiteConfig sqlite = new SQLiteConfig();
        sqlite.setReadOnly(true);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + candidate, sqlite.toProperties());
             Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("pragma integrity_check")) {
                if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) {
                    throw new IllegalArgumentException("La base SQLite n'est pas intègre.");
                }
            }
            for (String table : REQUIRED_TABLES) {
                if (!tableExists(statement, table)) {
                    throw new IllegalArgumentException("Base GesCal incompatible : table " + table + " absente.");
                }
            }
            Set<String> addressColumns = columns(statement, "adresses");
            if (!addressColumns.containsAll(Set.of("idadresse", "nomrue", "complement", "cp", "ville"))) {
                throw new IllegalArgumentException("Base GesCal incompatible : structure des adresses incomplète.");
            }
            if (addressColumns.contains("numrue")) {
                throw new IllegalArgumentException("Cette base utilise encore l'ancien champ numrue et doit être migrée.");
            }
        }
        return candidate;
    }

    private boolean tableExists(Statement statement, String table) throws Exception {
        try (ResultSet result = statement.executeQuery(
                "select count(*) from sqlite_master where type='table' and name='" + table + "'")) {
            return result.next() && result.getLong(1) > 0;
        }
    }

    private Set<String> columns(Statement statement, String table) throws Exception {
        java.util.HashSet<String> columns = new java.util.HashSet<>();
        try (ResultSet result = statement.executeQuery("pragma table_info(" + table + ")")) {
            while (result.next()) columns.add(result.getString("name").toLowerCase());
        }
        return columns;
    }

    private Optional<Path> chooseFile(Path current) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Le sélecteur de fichiers Windows n'est pas disponible dans ce mode d'exécution.");
        }
        AtomicReference<Optional<Path>> result = new AtomicReference<>(Optional.empty());
        Runnable dialog = () -> {
            JFrame parent = new JFrame();
            try {
                parent.setAlwaysOnTop(true);
                parent.setUndecorated(true);
                parent.setLocationRelativeTo(null);
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Choisir la base SQLite de GesCal");
                chooser.setFileFilter(new FileNameExtensionFilter("Bases SQLite (*.db, *.sqlite)", "db", "sqlite", "sqlite3"));
                Path directory = Files.isDirectory(current) ? current : current.getParent();
                if (directory != null && Files.isDirectory(directory)) chooser.setCurrentDirectory(directory.toFile());
                if (Files.isRegularFile(current)) chooser.setSelectedFile(current.toFile());
                if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    result.set(Optional.of(chooser.getSelectedFile().toPath()));
                }
            } finally {
                parent.dispose();
            }
        };
        if (javax.swing.SwingUtilities.isEventDispatchThread()) dialog.run();
        else javax.swing.SwingUtilities.invokeAndWait(dialog);
        return result.get();
    }

    private void persist(Path database) throws IOException {
        Path parent = configPath.getParent();
        if (parent != null) Files.createDirectories(parent);
        List<String> lines = Files.exists(configPath)
                ? new ArrayList<>(Files.readAllLines(configPath, StandardCharsets.UTF_8))
                : new ArrayList<>();
        String value = database.toString().replace('\\', '/');
        boolean replaced = false;
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).trim().startsWith(KEY + "=")) {
                lines.set(index, KEY + "=" + value);
                replaced = true;
                break;
            }
        }
        if (!replaced) lines.add(KEY + "=" + value);
        Path temporary = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path normalize(String value) {
        return Path.of(value).toAbsolutePath().normalize();
    }
}
