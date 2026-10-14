package com.editor.network;

import com.editor.sync.DocumentStorage;
import com.editor.sync.DocumentSync;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    private static class Room {
        final String roomId;
        final String password;
        final StringBuilder doc = new StringBuilder();
        final Set<ClientHandler> members = Collections.synchronizedSet(new HashSet<>());

        Room(String roomId, String password) {
            this.roomId = roomId;
            this.password = password == null ? "" : password;
            if ("default".equals(roomId)) {
                String saved = DocumentStorage.loadLocally();
                if (saved != null && !saved.isEmpty()) {
                    doc.append(saved);
                }
            }
        }
    }

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Server(int port) {
        this.port = port;
        rooms.put("default", new Room("default", ""));
    }

    public synchronized boolean start() {
        if (running) return true;
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Server started on port: " + port);

            Thread acceptThread = new Thread(this::listenForClients, "Server-Accept");
            acceptThread.setDaemon(true);
            acceptThread.start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void listenForClients() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                handler.start();
            } catch (IOException e) {
                if (!running) break;
            }
        }
    }

    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
    }

    public class ClientHandler extends Thread {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private Room currentRoom;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                // Default auto-join
                Room defaultRoom = rooms.get("default");
                this.currentRoom = defaultRoom;
                defaultRoom.members.add(this);

                synchronized (defaultRoom.doc) {
                    if (defaultRoom.doc.length() > 0) {
                        send(new DocumentSync(DocumentSync.ActionType.REPLACE_ALL, 0, defaultRoom.doc.length(), defaultRoom.doc.toString(), "SERVER"));
                    }
                }

                while (running) {
                    Object obj = in.readObject();
                    if (obj instanceof DocumentSync sync) {
                        handleSync(sync);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                leaveRoom();
            }
        }

        private void handleSync(DocumentSync sync) {
            if (sync.getActionType() == DocumentSync.ActionType.JOIN_ROOM) {
                String rId = (sync.getRoomId() == null || sync.getRoomId().trim().isEmpty()) ? "default" : sync.getRoomId().trim();
                String pwd = sync.getRoomPassword() == null ? "" : sync.getRoomPassword().trim();

                Room room = rooms.computeIfAbsent(rId, k -> new Room(rId, pwd));
                if (!room.password.isEmpty() && !room.password.equals(pwd)) {
                    send(new DocumentSync(DocumentSync.ActionType.AUTH_FAILED, 0, 0, "Invalid Room Password", "SERVER"));
                    return;
                }

                if (currentRoom != null) currentRoom.members.remove(this);
                this.currentRoom = room;
                room.members.add(this);

                send(new DocumentSync(DocumentSync.ActionType.AUTH_SUCCESS, 0, 0, rId, "SERVER"));
                synchronized (room.doc) {
                    send(new DocumentSync(DocumentSync.ActionType.REPLACE_ALL, 0, room.doc.length(), room.doc.toString(), "SERVER"));
                }
                return;
            }

            if (currentRoom == null) return;

            synchronized (currentRoom.doc) {
                if (sync.getActionType() == DocumentSync.ActionType.INSERT) {
                    int off = Math.min(sync.getOffset(), currentRoom.doc.length());
                    currentRoom.doc.insert(off, sync.getText());
                } else if (sync.getActionType() == DocumentSync.ActionType.DELETE) {
                    int start = Math.min(sync.getOffset(), currentRoom.doc.length());
                    int end = Math.min(start + sync.getLength(), currentRoom.doc.length());
                    if (start <= end) currentRoom.doc.delete(start, end);
                } else if (sync.getActionType() == DocumentSync.ActionType.REPLACE_ALL) {
                    currentRoom.doc.setLength(0);
                    currentRoom.doc.append(sync.getText());
                }

                if ("default".equals(currentRoom.roomId)) {
                    DocumentStorage.saveLocally(currentRoom.doc.toString());
                }
            }

            synchronized (currentRoom.members) {
                for (ClientHandler peer : currentRoom.members) {
                    if (peer != this) {
                        peer.send(sync);
                    }
                }
            }
        }

        private void leaveRoom() {
            if (currentRoom != null) {
                currentRoom.members.remove(this);
                currentRoom = null;
            }
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }

        public void send(DocumentSync sync) {
            try {
                if (out != null) {
                    out.writeObject(sync);
                    out.flush();
                }
            } catch (IOException ignored) {}
        }
    }
}