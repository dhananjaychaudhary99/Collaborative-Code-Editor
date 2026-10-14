package com.editor.network;

import com.editor.sync.DocumentSync;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client implements Runnable {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean running = false;
    private Consumer<DocumentSync> onSyncReceived;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setOnSyncReceived(Consumer<DocumentSync> callback) {
        this.onSyncReceived = callback;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void start() {
        Thread thread = new Thread(this, "Client-Thread");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            running = true;

            while (running && !socket.isClosed()) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof DocumentSync sync && onSyncReceived != null) {
                        onSyncReceived.accept(sync);
                    }
                } catch (ClassNotFoundException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("Client network issue: " + e.getMessage());
        } finally {
            close();
        }
    }

    public void send(DocumentSync sync) {
        try {
            if (out != null) {
                out.writeObject(sync);
                out.flush();
            }
        } catch (IOException ignored) {}
    }

    public void disconnect() {
        close();
    }

    public void close() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}