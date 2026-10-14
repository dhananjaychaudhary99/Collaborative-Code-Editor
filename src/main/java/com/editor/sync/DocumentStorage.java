package com.editor.sync;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class DocumentStorage {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path DATA_FILE = DATA_DIR.resolve("saved_code.txt");
    private static final Path META_FILE = DATA_DIR.resolve("saved_meta.properties");
    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;

    public static synchronized void saveLocally(String content) {
        try {
            if (!Files.exists(DATA_DIR)) {
                Files.createDirectories(DATA_DIR);
            }
            Files.writeString(DATA_FILE, content, StandardCharsets.UTF_8);
            String meta = "timestamp=" + System.currentTimeMillis();
            Files.writeString(META_FILE, meta, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    public static synchronized String loadLocally() {
        try {
            if (!Files.exists(DATA_FILE) || !Files.exists(META_FILE)) {
                return "";
            }
            String meta = Files.readString(META_FILE, StandardCharsets.UTF_8);
            long savedTime = 0;
            for (String line : meta.split("\n")) {
                if (line.startsWith("timestamp=")) {
                    savedTime = Long.parseLong(line.substring("timestamp=".length()).trim());
                    break;
                }
            }
            if (System.currentTimeMillis() - savedTime > THIRTY_DAYS_MS) {
                deleteLocally();
                return "";
            }
            return Files.readString(DATA_FILE, StandardCharsets.UTF_8);
        } catch (Exception e) {
            deleteLocally();
            return "";
        }
    }

    public static synchronized void deleteLocally() {
        try {
            Files.deleteIfExists(DATA_FILE);
            Files.deleteIfExists(META_FILE);
        } catch (IOException ignored) {}
    }
}