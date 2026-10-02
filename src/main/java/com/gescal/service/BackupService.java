package com.gescal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BackupService {
    private final JdbcTemplate jdbc;
    private final Path backupDirectory;

    public BackupService(JdbcTemplate jdbc, @Value("${gescal.backup.directory}") String backupDirectory) {
        this.jdbc = jdbc;
        this.backupDirectory = Path.of(backupDirectory);
    }

    public Path backup() throws Exception {
        Files.createDirectories(backupDirectory);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path destination = backupDirectory.resolve("gescal_" + stamp + ".db").toAbsolutePath().normalize();
        String escaped = destination.toString().replace("\\", "/").replace("'", "''");
        jdbc.execute("VACUUM INTO '" + escaped + "'");
        return destination;
    }
}

