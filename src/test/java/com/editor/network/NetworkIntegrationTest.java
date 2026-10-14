package com.editor.network;

import com.editor.sync.DocumentSync;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class NetworkIntegrationTest {

    private int testPort;
    private Server server;
    private Client client1;
    private Client client2;

    @BeforeEach
    void setUp() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            testPort = socket.getLocalPort();
        }

        server = new Server(testPort);
        boolean started = server.start();
        assertTrue(started, "Server must bind and start listening");

        client1 = new Client("localhost", testPort);
        client2 = new Client("localhost", testPort);
    }

    @AfterEach
    void tearDown() {
        if (client1 != null) client1.disconnect();
        if (client2 != null) client2.disconnect();
        if (server != null) server.stop();
    }

    @Test
    @DisplayName("Broadcast routing: message sent by Client 1 must be received by Client 2")
    void testMessageBroadcastBetweenClients() throws Exception {
        client1.start();
        client2.start();

        Thread.sleep(200);

        assertTrue(client1.isConnected(), "Client 1 should be connected");
        assertTrue(client2.isConnected(), "Client 2 should be connected");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<DocumentSync> receivedSync = new AtomicReference<>();

        client2.setOnSyncReceived(sync -> {
            receivedSync.set(sync);
            latch.countDown();
        });

        DocumentSync outbound = new DocumentSync(
            DocumentSync.ActionType.INSERT,
            0,
            4,
            "void",
            "test-client-1"
        );

        client1.send(outbound);

        boolean reached = latch.await(3, TimeUnit.SECONDS);
        assertTrue(reached, "Client 2 did not receive the sync message in time");

        DocumentSync received = receivedSync.get();
        assertNotNull(received);
        assertEquals(DocumentSync.ActionType.INSERT, received.getActionType());
        assertEquals(0, received.getOffset());
        assertEquals("void", received.getText());
        assertEquals("test-client-1", received.getClientId());
    }

    @Test
    @DisplayName("Echo suppression: sender must not receive its own broadcast")
    void testSenderDoesNotReceiveEcho() throws Exception {
        client1.start();
        Thread.sleep(200);

        CountDownLatch latch = new CountDownLatch(1);
        client1.setOnSyncReceived(sync -> latch.countDown());

        DocumentSync outbound = new DocumentSync(
            DocumentSync.ActionType.DELETE,
            5,
            2,
            "",
            "test-client-1"
        );

        client1.send(outbound);

        boolean echoed = latch.await(600, TimeUnit.MILLISECONDS);
        assertFalse(echoed, "Sender received its own broadcast message");
    }

    @Test
    @DisplayName("Should gracefully handle disconnection")
    void testClientDisconnect() throws Exception {
        client1.start();
        Thread.sleep(100);
        assertTrue(client1.isConnected());

        client1.disconnect();
        assertFalse(client1.isConnected());
    }
}