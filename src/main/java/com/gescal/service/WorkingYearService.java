package com.gescal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WorkingYearService {
    private static final String KEY = "gescal.working-year";
    private final AtomicInteger workingYear;
    private final Path configPath;

    public WorkingYearService(@Value("${gescal.working-year:0}") int configuredYear,
                              @Value("${gescal.config.path:./config/gescal.properties}") String configPath) {
        this.workingYear = new AtomicInteger(configuredYear == 0 ? Year.now().getValue() : validate(configuredYear));
        this.configPath = Path.of(configPath).toAbsolutePath().normalize();
    }

    public int get() {
        return workingYear.get();
    }

    public synchronized void setAndPersist(int year) throws IOException {
        int validated = validate(year);
        Path parent = configPath.getParent();
        if (parent != null) Files.createDirectories(parent);
        List<String> lines = Files.exists(configPath)
                ? new ArrayList<>(Files.readAllLines(configPath, StandardCharsets.UTF_8))
                : new ArrayList<>();
        boolean replaced = false;
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).trim().startsWith(KEY + "=")) {
                lines.set(index, KEY + "=" + validated);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) lines.add("");
            lines.add("# Année utilisée par défaut dans toute l'application");
            lines.add(KEY + "=" + validated);
        }
        Path temporary = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
        }
        workingYear.set(validated);
    }

    private static int validate(int year) {
        if (year < 2000 || year > 9999) throw new IllegalArgumentException("L'année de travail doit contenir 4 chiffres.");
        return year;
    }
}
