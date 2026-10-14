package com.editor;

import com.editor.network.Client;
import com.editor.network.Server;
import com.editor.ui.EditorFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }

        String mode = args[0].toLowerCase();
        if ("server".equals(mode)) {
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
            Server server = new Server(port);
            server.start();
            try {
                Thread.currentThread().join();
            } catch (InterruptedException ignored) {}
        } else if ("client".equals(mode)) {
            String host = args.length > 1 ? args[1] : "localhost";
            int port = args.length > 2 ? Integer.parseInt(args[2]) : 8080;

            SwingUtilities.invokeLater(() -> {
                Client client = new Client(host, port);
                EditorFrame frame = new EditorFrame(client);
                frame.setVisible(true);
                client.start();
            });
        }
    }
}