package com.editor.network;

import com.editor.sync.DocumentSync;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<DocumentSync> onSyncReceived;
    private volatile boolean connected = false;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setOnSyncReceived(Consumer<DocumentSync> onSyncReceived) {
        this.onSyncReceived = onSyncReceived;
    }

    public void start() throws IOException {
        this.socket = new Socket(host, port);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        this.connected = true;

        Thread listenerThread = new Thread(this::listen, "Client-Listener-Thread");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listen() {
        try {
            while (connected) {
                DocumentSync sync = (DocumentSync) in.readObject();
                if (onSyncReceived != null) {
                    onSyncReceived.accept(sync);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            disconnect();
        }
    }

    public synchronized void send(DocumentSync sync) {
        if (!connected || out == null) return;
        try {
            out.writeObject(sync);
            out.flush();
        } catch (IOException e) {
            disconnect();
        }
    }

    public synchronized void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return connected;
    }
}