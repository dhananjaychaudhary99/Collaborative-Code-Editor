package com.editor.sync;

import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentStorageTest {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path DATA_FILE = DATA_DIR.resolve("saved_code.txt");
    private static final Path META_FILE = DATA_DIR.resolve("saved_meta.properties");

    @BeforeEach
    public void setup() {
        DocumentStorage.deleteLocally();
    }

    @AfterEach
    public void tearDown() {
        DocumentStorage.deleteLocally();
    }

    @Test
    public void testSaveAndLoadWithinRetention() {
        String code = "System.out.println(\"Retention Test\");";
        DocumentStorage.saveLocally(code);

        String loaded = DocumentStorage.loadLocally();
        assertEquals(code, loaded, "Loaded code should match saved code within 30-day retention window.");
    }

    @Test
    public void testExplicitDeletePermanentlyWipesFiles() {
        DocumentStorage.saveLocally("Some text to delete");
        assertTrue(Files.exists(DATA_FILE), "Data file should exist after saving.");

        DocumentStorage.deleteLocally();
        assertFalse(Files.exists(DATA_FILE), "Data file must be deleted.");
        assertFalse(Files.exists(META_FILE), "Metadata file must be deleted.");
        assertEquals("", DocumentStorage.loadLocally(), "Loading after delete must return empty string.");
    }

    @Test
    public void testExpiryAfterThirtyDays() throws IOException {
        DocumentStorage.saveLocally("Expiring code");

        long thirtyOneDaysAgo = System.currentTimeMillis() - (31L * 24 * 60 * 60 * 1000);
        Files.writeString(META_FILE, "timestamp=" + thirtyOneDaysAgo, StandardCharsets.UTF_8);

        String loaded = DocumentStorage.loadLocally();
        assertEquals("", loaded, "Data older than 30 days must be expired and return empty.");
        assertFalse(Files.exists(DATA_FILE), "Expired files must be auto-deleted from disk.");
    }
}