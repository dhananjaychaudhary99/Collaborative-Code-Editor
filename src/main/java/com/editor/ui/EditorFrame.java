package com.editor.ui;

import com.editor.network.Client;
import com.editor.sync.DocumentSync;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.util.UUID;

public class EditorFrame extends JFrame {
    private final RSyntaxTextArea textArea;
    private final Client client;
    private final JLabel statusLabel;
    private final String localClientId;
    private final RemoteCaretPainter remoteCaretPainter;
    private volatile boolean isRemoteUpdate = false;

    public EditorFrame(Client client) {
        this.client = client;
        this.localClientId = "User-" + UUID.randomUUID().toString().substring(0, 4);

        setTitle("Collaborative Code Editor [" + localClientId + "]");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Subclass RSyntaxTextArea to render remote carets on top
        textArea = new RSyntaxTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (remoteCaretPainter != null) {
                    remoteCaretPainter.paintRemoteCarets((Graphics2D) g);
                }
            }
        };

        SyntaxHighlighter.applyDefaults(textArea);
        remoteCaretPainter = new RemoteCaretPainter(textArea);

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusLabel = new JLabel(" Status: " + (client.isConnected() ? "Connected" : "Disconnected") + " | ID: " + localClientId);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusBar.add(statusLabel, BorderLayout.WEST);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        String[] languages = {"Java", "Python", "C++", "JavaScript", "HTML", "JSON"};
        JComboBox<String> langDropdown = new JComboBox<>(languages);
        langDropdown.addActionListener(e -> {
            String selected = (String) langDropdown.getSelectedItem();
            if (selected != null) {
                SyntaxHighlighter.setLanguage(textArea, selected);
            }
        });
        toolBar.add(new JLabel(" Language: "));
        toolBar.add(langDropdown);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        attachDocumentListener();
        attachCaretListener();

        client.setOnSyncReceived(this::handleRemoteSync);
    }

    private void attachDocumentListener() {
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (isRemoteUpdate) return;
                try {
                    String insertedText = e.getDocument().getText(e.getOffset(), e.getLength());
                    client.send(new DocumentSync(
                        DocumentSync.ActionType.INSERT,
                        e.getOffset(),
                        e.getLength(),
                        insertedText,
                        localClientId
                    ));
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (isRemoteUpdate) return;
                client.send(new DocumentSync(
                    DocumentSync.ActionType.DELETE,
                    e.getOffset(),
                    e.getLength(),
                    "",
                    localClientId
                ));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    private void attachCaretListener() {
        textArea.addCaretListener(new CaretListener() {
            private int lastPos = -1;

            @Override
            public void caretUpdate(CaretEvent e) {
                if (isRemoteUpdate) return;
                int dot = e.getDot();
                if (dot != lastPos) {
                    lastPos = dot;
                    client.send(new DocumentSync(DocumentSync.ActionType.CURSOR_MOVE, localClientId, dot));
                }
            }
        });
    }

    private void handleRemoteSync(DocumentSync sync) {
        SwingUtilities.invokeLater(() -> {
            if (sync.getActionType() == DocumentSync.ActionType.CURSOR_MOVE) {
                remoteCaretPainter.updatePeerCursor(sync.getClientId(), sync.getCursorPosition());
                return;
            }

            if (sync.getActionType() == DocumentSync.ActionType.CLIENT_DISCONNECT) {
                remoteCaretPainter.removePeer(sync.getClientId());
                return;
            }

            isRemoteUpdate = true;
            try {
                if (sync.getActionType() == DocumentSync.ActionType.INSERT) {
                    textArea.getDocument().insertString(sync.getOffset(), sync.getText(), null);
                } else if (sync.getActionType() == DocumentSync.ActionType.DELETE) {
                    textArea.getDocument().remove(sync.getOffset(), sync.getLength());
                } else if (sync.getActionType() == DocumentSync.ActionType.REPLACE_ALL) {
                    textArea.setText(sync.getText());
                }
                // Reposition remote cursor behind the mutation
                remoteCaretPainter.updatePeerCursor(sync.getClientId(), sync.getOffset() + sync.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            } finally {
                isRemoteUpdate = false;
            }
        });
    }

    public void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(" Status: " + status + " | ID: " + localClientId));
    }
}