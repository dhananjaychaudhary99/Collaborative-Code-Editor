package com.editor;

import com.editor.network.Client;
import com.editor.network.Server;
import com.editor.ui.EditorFrame;

import javax.swing.*;
import java.io.IOException;

public class Main {
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOST = "localhost";

    public static void main(String[] args) {
        if (args.length > 0) {
            String mode = args[0].toLowerCase();
            switch (mode) {
                case "server":
                    int serverPort = (args.length > 1) ? Integer.parseInt(args[1]) : DEFAULT_PORT;
                    startDedicatedServer(serverPort);
                    return;
                case "client":
                    String host = (args.length > 1) ? args[1] : DEFAULT_HOST;
                    int clientPort = (args.length > 2) ? Integer.parseInt(args[2]) : DEFAULT_PORT;
                    startClient(host, clientPort);
                    return;
                default:
                    System.err.println("Invalid mode. Usage: java -jar app.jar [server|client] [args...]");
                    System.exit(1);
            }
        }

        SwingUtilities.invokeLater(Main::showStartupDialog);
    }

    private static void startDedicatedServer(int port) {
        System.out.println("Starting dedicated server on port " + port + "...");
        Server server = new Server(port);
        server.start();
    }

    private static void startClient(String host, int port) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client(host, port);
            EditorFrame frame = new EditorFrame(client);
            frame.setVisible(true);

            new Thread(() -> {
                try {
                    client.start();
                    frame.updateStatus("Connected (" + host + ":" + port + ")");
                } catch (IOException e) {
                    frame.updateStatus("Failed to connect to " + host + ":" + port);
                    JOptionPane.showMessageDialog(frame,
                            "Could not connect to server at " + host + ":" + port + "\n" + e.getMessage(),
                            "Connection Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }, "Client-Connector-Thread").start();
        });
    }

    private static void showStartupDialog() {
        String[] options = {"Host & Join (Local)", "Join Remote Server", "Dedicated Server Only"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Select run mode for Collaborative Code Editor:",
                "Start Collaborative Editor",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        switch (choice) {
            case 0:
                new Server(DEFAULT_PORT).start();
                startClient(DEFAULT_HOST, DEFAULT_PORT);
                break;
            case 1:
                String host = JOptionPane.showInputDialog(null, "Server Host:", DEFAULT_HOST);
                if (host != null && !host.trim().isEmpty()) {
                    startClient(host.trim(), DEFAULT_PORT);
                }
                break;
            case 2:
                startDedicatedServer(DEFAULT_PORT);
                break;
            default:
                System.exit(0);
        }
    }
}