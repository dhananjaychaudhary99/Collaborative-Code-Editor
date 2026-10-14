package com.editor.ui;

import com.editor.network.Client;
import com.editor.sync.CodeRunner;
import com.editor.sync.DocumentStorage;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.UUID;

public class EditorFrame extends JFrame {
    private final RSyntaxTextArea textArea;
    private final JTextArea consoleArea;
    private final Client client;
    private final JLabel statusLabel;
    private final String localClientId;
    private final RemoteCaretPainter remoteCaretPainter;
    private String currentRoom = "default";
    private volatile boolean isRemoteUpdate = false;

    public static final String[] SUPPORTED_LANGUAGES = {
        "Java", "Python", "C++", "C", "C#", "JavaScript", "TypeScript", 
        "Rust", "Go", "Kotlin", "Scala", "Ruby", "PHP", "Groovy", "ActionScript", 
        "Perl", "Lua", "Dart", "SQL", "HTML", "CSS", "JSON", "XML", 
        "YAML", "Markdown", "Bash/Shell", "Batch", "Properties", 
        "Assembly (x86)", "Dockerfile"
    };

    public EditorFrame(Client client) {
        this.client = client;
        this.localClientId = "User-" + UUID.randomUUID().toString().substring(0, 4);

        setTitle("Collaborative Code Editor - [" + localClientId + "] | Room: " + currentRoom);
        setSize(1150, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client != null) client.close();
                System.exit(0);
            }
        });

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

        RTextScrollPane editorScrollPane = new RTextScrollPane(textArea);
        editorScrollPane.setLineNumbersEnabled(true);

        consoleArea = new JTextArea(8, 20);
        consoleArea.setEditable(false);
        consoleArea.setBackground(new Color(24, 24, 24));
        consoleArea.setForeground(new Color(133, 193, 233));
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        consoleArea.setText("--- Terminal Output Console ---\n");

        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, consoleScrollPane);
        splitPane.setResizeWeight(0.75);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JComboBox<String> langDropdown = new JComboBox<>(SUPPORTED_LANGUAGES);
        langDropdown.setMaximumRowCount(15);
        langDropdown.addActionListener(e -> {
            String selected = (String) langDropdown.getSelectedItem();
            if (selected != null) {
                SyntaxHighlighter.setLanguage(textArea, selected);
                if (textArea.getText().trim().isEmpty() || isDefaultTemplate(textArea.getText())) {
                    setBoilerplate(selected);
                }
            }
        });

        JButton runBtn = new JButton("Run Code");
        runBtn.setBackground(new Color(39, 174, 96));
        runBtn.setForeground(Color.WHITE);
        runBtn.setFocusPainted(false);
        runBtn.addActionListener(e -> runActiveCode((String) langDropdown.getSelectedItem()));

        JButton clearConsoleBtn = new JButton("Clear Console");
        clearConsoleBtn.setFocusPainted(false);
        clearConsoleBtn.addActionListener(e -> consoleArea.setText("--- Terminal Output Console ---\n"));

        JButton roomBtn = new JButton("Join Room");
        roomBtn.setFocusPainted(false);
        roomBtn.addActionListener(e -> promptJoinRoom());

        JButton deleteBtn = new JButton("Delete Local Data");
        deleteBtn.setBackground(new Color(192, 57, 43));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFocusPainted(false);
        deleteBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Are you sure you want to permanently delete saved 30-day data?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                DocumentStorage.deleteLocally();
                textArea.setText("");
                client.send(new DocumentSync(DocumentSync.ActionType.REPLACE_ALL, 0, 0, "", localClientId));
                consoleArea.append("\n[Storage]: Local files permanently wiped.\n");
            }
        });

        toolBar.add(new JLabel(" Language (30): "));
        toolBar.add(langDropdown);
        toolBar.addSeparator();
        toolBar.add(runBtn);
        toolBar.addSeparator();
        toolBar.add(clearConsoleBtn);
        toolBar.addSeparator();
        toolBar.add(roomBtn);
        toolBar.addSeparator();
        toolBar.add(deleteBtn);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusLabel = new JLabel(" Status: Connected | Room: " + currentRoom + " | ID: " + localClientId + " | Auto-save: 30 Days");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusBar.add(statusLabel, BorderLayout.WEST);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        String saved = DocumentStorage.loadLocally();
        if (saved != null && !saved.trim().isEmpty()) {
            textArea.setText(saved);
        } else {
            setBoilerplate("Java");
        }

        attachDocumentListener();
        attachCaretListener();

        client.setOnSyncReceived(this::handleRemoteSync);
    }

    private boolean isDefaultTemplate(String text) {
        return text.contains("Hello from") || text.contains("Start typing");
    }

    private void setBoilerplate(String lang) {
        String template = switch (lang.toLowerCase()) {
            case "java" -> "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello from Java!\");\n    }\n}\n";
            case "python" -> "# Python 3\nprint(\"Hello from Python!\")\n";
            case "c++" -> "#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"Hello from C++!\" << endl;\n    return 0;\n}\n";
            case "c" -> "#include <stdio.h>\n\nint main() {\n    printf(\"Hello from C!\\n\");\n    return 0;\n}\n";
            case "c#" -> "using System;\n\nclass Program {\n    static void Main() {\n        Console.WriteLine(\"Hello from C#!\");\n    }\n}\n";
            case "javascript" -> "console.log(\"Hello from JavaScript!\");\n";
            case "typescript" -> "const message: string = \"Hello from TypeScript!\";\nconsole.log(message);\n";
            case "rust" -> "fn main() {\n    println!(\"Hello from Rust!\");\n}\n";
            case "go" -> "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello from Go!\")\n}\n";
            case "kotlin" -> "fun main() {\n    println(\"Hello from Kotlin!\")\n}\n";
            case "scala" -> "object Main extends App {\n    println(\"Hello from Scala!\")\n}\n";
            case "ruby" -> "puts \"Hello from Ruby!\"\n";
            case "php" -> "<?php\necho \"Hello from PHP!\";\n?>\n";
            case "groovy" -> "println \"Hello from Groovy!\"\n";
            case "actionscript" -> "trace(\"Hello from ActionScript!\");\n";
            case "perl" -> "print \"Hello from Perl!\\n\";\n";
            case "lua" -> "print(\"Hello from Lua!\")\n";
            case "dart" -> "void main() {\n    print('Hello from Dart!');\n}\n";
            case "sql" -> "SELECT 'Hello from SQL!' AS greeting;\n";
            case "html" -> "<!DOCTYPE html>\n<html>\n<head><title>Page</title></head>\n<body>\n    <h1>Hello from HTML!</h1>\n</body>\n</html>\n";
            case "css" -> "body {\n    background-color: #1e1e1e;\n    color: #ffffff;\n}\n";
            case "json" -> "{\n  \"message\": \"Hello from JSON!\",\n  \"active\": true\n}\n";
            case "xml" -> "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n    <greeting>Hello from XML!</greeting>\n</root>\n";
            case "yaml" -> "message: Hello from YAML!\nversion: 1.0\n";
            case "markdown" -> "# Hello from Markdown\n- Collaborative Editor\n- 30-Day Persistence\n";
            case "bash/shell" -> "#!/bin/bash\necho \"Hello from Bash!\"\n";
            case "batch" -> "@echo off\necho Hello from Batch!\npause\n";
            case "properties" -> "app.name=CollaborativeEditor\napp.version=1.0.0\n";
            case "assembly (x86)" -> "section .text\nglobal _start\n_start:\n    mov eax, 1\n    int 0x80\n";
            case "dockerfile" -> "FROM openjdk:17-alpine\nWORKDIR /app\nCMD [\"java\", \"-version\"]\n";
            default -> "// Start typing here...\n";
        };
        textArea.setText(template);
    }

    private void runActiveCode(String lang) {
        consoleArea.append("\n[Executing " + lang + " code...]\n");
        new Thread(() -> {
            String result = CodeRunner.execute(textArea.getText(), lang);
            SwingUtilities.invokeLater(() -> consoleArea.append(result + "\n----------------------------------------\n"));
        }).start();
    }

    private void promptJoinRoom() {
        JTextField roomField = new JTextField("project-1");
        JPasswordField passField = new JPasswordField();
        Object[] msg = { "Room ID:", roomField, "Password (Optional):", passField };

        int opt = JOptionPane.showConfirmDialog(this, msg, "Join Private Room", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            String rId = roomField.getText().trim();
            String pwd = new String(passField.getPassword()).trim();
            client.send(new DocumentSync(DocumentSync.ActionType.JOIN_ROOM, rId, pwd, localClientId));
        }
    }

    private void attachDocumentListener() {
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (isRemoteUpdate) return;
                try {
                    String text = e.getDocument().getText(e.getOffset(), e.getLength());
                    client.send(new DocumentSync(DocumentSync.ActionType.INSERT, e.getOffset(), e.getLength(), text, localClientId));
                } catch (BadLocationException ignored) {}
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                if (isRemoteUpdate) return;
                client.send(new DocumentSync(DocumentSync.ActionType.DELETE, e.getOffset(), e.getLength(), "", localClientId));
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
            if (sync.getActionType() == DocumentSync.ActionType.AUTH_SUCCESS) {
                currentRoom = sync.getText();
                setTitle("Collaborative Code Editor - [" + localClientId + "] | Room: " + currentRoom);
                statusLabel.setText(" Status: Connected | Room: " + currentRoom + " | ID: " + localClientId + " | Auto-save: 30 Days");
                consoleArea.append("\n[Network]: Joined Room '" + currentRoom + "' successfully.\n");
                return;
            }

            if (sync.getActionType() == DocumentSync.ActionType.AUTH_FAILED) {
                JOptionPane.showMessageDialog(this, sync.getText(), "Authentication Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (sync.getActionType() == DocumentSync.ActionType.CURSOR_MOVE) {
                remoteCaretPainter.updatePeerCursor(sync.getClientId(), sync.getCursorPosition());
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
            } catch (BadLocationException ignored) {
            } finally {
                isRemoteUpdate = false;
            }
        });
    }
}