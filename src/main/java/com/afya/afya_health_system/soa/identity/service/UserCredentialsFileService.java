package com.afya.afya_health_system.soa.identity.service;

import com.afya.afya_health_system.soa.identity.dto.CredentialsLogPreviewResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Appends newly generated credentials to a local TSV file and a parallel CSV file.
 */
@Service
public class UserCredentialsFileService {

    private static final String TXT_HEADER =
            "# CONFIDENTIEL — Mots de passe en clair. À sécuriser (accès disque, sauvegardes)." + System.lineSeparator()
                    + "# ISO8601\tUsername\tNom complet\tMot de passe" + System.lineSeparator();

    private static final String CSV_HEADER = "createdAt,username,fullName,password" + System.lineSeparator();

    private final Path logFile;
    private final Path csvFile;

    public UserCredentialsFileService(
            @Value("${app.users.credentials-log-path:./data/generated-user-credentials.txt}") String pathStr
    ) {
        this.logFile = Paths.get(pathStr).toAbsolutePath().normalize();
        this.csvFile = resolveCsvSibling(this.logFile);
    }

    private static Path resolveCsvSibling(Path txt) {
        String s = txt.toString();
        if (s.endsWith(".txt")) {
            return Paths.get(s.substring(0, s.length() - 4) + ".csv").normalize();
        }
        Path parent = txt.getParent();
        return parent != null ? parent.resolve("generated-user-credentials.csv") : Paths.get("generated-user-credentials.csv");
    }

    public Path getLogFilePath() {
        return logFile;
    }

    public Path getCsvFilePath() {
        return csvFile;
    }

    public boolean logFileExists() {
        return Files.isRegularFile(logFile) && fileNonEmpty(logFile);
    }

    public boolean csvLogExists() {
        return Files.isRegularFile(csvFile) && fileNonEmpty(csvFile);
    }

    private static boolean fileNonEmpty(Path p) {
        try {
            return Files.size(p) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public Resource asDownloadableResource() {
        return new FileSystemResource(logFile.toFile());
    }

    public Resource asCsvDownloadableResource() {
        return new FileSystemResource(csvFile.toFile());
    }

    /**
     * Reads TSV (preferred) or CSV for UI preview.
     */
    public synchronized CredentialsLogPreviewResponse buildPreview(int maxPreviewBytes) {
        int cap = Math.max(1024, maxPreviewBytes);
        try {
            Path source = pickPreviewSource();
            if (source == null) {
                return new CredentialsLogPreviewResponse("", true, false, 0, 0);
            }
            long totalBytes = Files.size(source);
            if (totalBytes == 0) {
                return new CredentialsLogPreviewResponse("", true, false, 0, 0);
            }
            byte[] all = Files.readAllBytes(source);
            String fullText = new String(all, StandardCharsets.UTF_8);
            int lineCount = countLines(fullText);
            if (all.length <= cap) {
                return new CredentialsLogPreviewResponse(fullText, false, false, totalBytes, lineCount);
            }
            String truncatedText = new String(all, 0, cap, StandardCharsets.UTF_8)
                    + System.lineSeparator()
                    + System.lineSeparator()
                    + "--- Aperçu tronqué (" + totalBytes + " octets au total) ---";
            return new CredentialsLogPreviewResponse(truncatedText, false, true, totalBytes, lineCount);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier des comptes.", e);
        }
    }

    private Path pickPreviewSource() throws IOException {
        if (Files.isRegularFile(logFile) && Files.size(logFile) > 0) {
            return logFile;
        }
        if (Files.isRegularFile(csvFile) && Files.size(csvFile) > 0) {
            return csvFile;
        }
        return null;
    }

    public synchronized void deleteLog() {
        try {
            Files.deleteIfExists(logFile);
            Files.deleteIfExists(csvFile);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de supprimer les fichiers des comptes.", e);
        }
    }

    private static int countLines(String s) {
        if (s.isEmpty()) {
            return 0;
        }
        int n = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    /**
     * Thread-safe append for concurrent user creation.
     */
    public synchronized void appendEntry(String username, String fullName, String plainPassword) {
        try {
            Path parent = logFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String instant = Instant.now().toString();
            appendTxtLine(instant, username, fullName, plainPassword);
            appendCsvLine(instant, username, fullName, plainPassword);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'écrire le fichier des comptes générés.", e);
        }
    }

    private void appendTxtLine(String instant, String username, String fullName, String plainPassword) throws IOException {
        if (!Files.exists(logFile)) {
            Files.writeString(logFile, TXT_HEADER, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        String line = instant
                + "\t"
                + sanitizeField(username)
                + "\t"
                + sanitizeField(fullName)
                + "\t"
                + sanitizeField(plainPassword)
                + System.lineSeparator();
        Files.writeString(logFile, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private void appendCsvLine(String instant, String username, String fullName, String plainPassword) throws IOException {
        if (!Files.exists(csvFile)) {
            Files.writeString(csvFile, CSV_HEADER, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        String row = csvEscape(instant)
                + ","
                + csvEscape(username)
                + ","
                + csvEscape(fullName)
                + ","
                + csvEscape(plainPassword)
                + System.lineSeparator();
        Files.writeString(csvFile, row, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private static String csvEscape(String s) {
        if (s == null) {
            return "\"\"";
        }
        String t = s.replace("\"", "\"\"");
        return "\"" + t + "\"";
    }

    private static String sanitizeField(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }
}
