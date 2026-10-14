package com.editor.network;

import com.editor.sync.DocumentSync;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Server {
    private final int port;
    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private volatile boolean isRunning = true;
    private ServerSocket serverSocket;
    private final CountDownLatch startLatch = new CountDownLatch(1);

    public Server(int port) {
        this.port = port;
    }

    public boolean start() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server listening on port: " + port);
                startLatch.countDown();
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket);
                    clients.add(handler);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                startLatch.countDown();
                if (isRunning) {
                    System.err.println("Server error: " + e.getMessage());
                }
            }
        }, "Server-Accept-Thread").start();

        try {
            return startLatch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        for (ClientHandler client : clients) {
            client.cleanup();
        }
        clients.clear();
    }

    public void broadcast(DocumentSync syncData, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(syncData);
            }
        }
    }

    public class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                while (isRunning) {
                    DocumentSync syncData = (DocumentSync) in.readObject();
                    broadcast(syncData, this);
                }
            } catch (IOException | ClassNotFoundException ignored) {
            } finally {
                cleanup();
            }
        }

        public void sendMessage(DocumentSync syncData) {
            try {
                if (out != null) {
                    out.writeObject(syncData);
                    out.flush();
                }
            } catch (IOException e) {
                cleanup();
            }
        }

        public void cleanup() {
            clients.remove(this);
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}