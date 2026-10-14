package com.editor.sync;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class DocumentSyncTest {

    @Test
    @DisplayName("Should correctly retain constructor values for edits and cursors")
    void testFieldRetention() {
        DocumentSync edit = new DocumentSync(DocumentSync.ActionType.INSERT, 10, 5, "hello", "Client-A");
        assertEquals(DocumentSync.ActionType.INSERT, edit.getActionType());
        assertEquals(10, edit.getOffset());
        assertEquals(5, edit.getLength());
        assertEquals("hello", edit.getText());
        assertEquals("Client-A", edit.getClientId());

        DocumentSync cursor = new DocumentSync(DocumentSync.ActionType.CURSOR_MOVE, "Client-B", 25);
        assertEquals(DocumentSync.ActionType.CURSOR_MOVE, cursor.getActionType());
        assertEquals("Client-B", cursor.getClientId());
        assertEquals(25, cursor.getCursorPosition());
    }

    @Test
    @DisplayName("Should serialize and deserialize without data loss")
    void testSerializationCycle() throws IOException, ClassNotFoundException {
        DocumentSync original = new DocumentSync(DocumentSync.ActionType.DELETE, 42, 7, "", "Client-X");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
            oos.flush();
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DocumentSync deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            deserialized = (DocumentSync) ois.readObject();
        }

        assertNotNull(deserialized);
        assertEquals(original.getActionType(), deserialized.getActionType());
        assertEquals(original.getOffset(), deserialized.getOffset());
        assertEquals(original.getLength(), deserialized.getLength());
        assertEquals(original.getText(), deserialized.getText());
        assertEquals(original.getClientId(), deserialized.getClientId());
    }
}