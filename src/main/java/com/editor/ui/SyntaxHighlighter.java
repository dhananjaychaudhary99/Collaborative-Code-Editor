package com.editor.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;

import java.awt.*;
import java.io.IOException;

public class SyntaxHighlighter {

    public static void applyDefaults(RSyntaxTextArea textArea) {
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setAutoIndentEnabled(true);
        textArea.setTabSize(4);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));

        try {
            Theme theme = Theme.load(SyntaxHighlighter.class.getResourceAsStream(
                "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
            theme.apply(textArea);
        } catch (IOException | NullPointerException e) {
            textArea.setBackground(new Color(30, 30, 30));
            textArea.setForeground(new Color(220, 220, 220));
            textArea.setCaretColor(Color.WHITE);
            textArea.setCurrentLineHighlightColor(new Color(45, 45, 45));
        }
    }

    public static void setLanguage(RSyntaxTextArea textArea, String language) {
        String style = switch (language.toLowerCase()) {
            case "java" -> SyntaxConstants.SYNTAX_STYLE_JAVA;
            case "python" -> SyntaxConstants.SYNTAX_STYLE_PYTHON;
            case "c++" -> SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
            case "c" -> SyntaxConstants.SYNTAX_STYLE_C;
            case "c#" -> SyntaxConstants.SYNTAX_STYLE_CSHARP;
            case "javascript" -> SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
            case "typescript" -> SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT;
            case "rust" -> SyntaxConstants.SYNTAX_STYLE_RUST;
            case "go" -> SyntaxConstants.SYNTAX_STYLE_GO;
            case "kotlin" -> SyntaxConstants.SYNTAX_STYLE_KOTLIN;
            case "scala" -> SyntaxConstants.SYNTAX_STYLE_SCALA;
            case "ruby" -> SyntaxConstants.SYNTAX_STYLE_RUBY;
            case "php" -> SyntaxConstants.SYNTAX_STYLE_PHP;
            case "groovy" -> SyntaxConstants.SYNTAX_STYLE_GROOVY;
            case "actionscript" -> SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT;
            case "perl" -> SyntaxConstants.SYNTAX_STYLE_PERL;
            case "lua" -> SyntaxConstants.SYNTAX_STYLE_LUA;
            case "dart" -> SyntaxConstants.SYNTAX_STYLE_DART;
            case "sql" -> SyntaxConstants.SYNTAX_STYLE_SQL;
            case "html" -> SyntaxConstants.SYNTAX_STYLE_HTML;
            case "css" -> SyntaxConstants.SYNTAX_STYLE_CSS;
            case "json" -> SyntaxConstants.SYNTAX_STYLE_JSON;
            case "xml" -> SyntaxConstants.SYNTAX_STYLE_XML;
            case "yaml" -> SyntaxConstants.SYNTAX_STYLE_YAML;
            case "markdown" -> SyntaxConstants.SYNTAX_STYLE_MARKDOWN;
            case "bash/shell" -> SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL;
            case "batch" -> SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH;
            case "properties" -> SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;
            case "assembly (x86)" -> SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86;
            case "dockerfile" -> SyntaxConstants.SYNTAX_STYLE_DOCKERFILE;
            default -> SyntaxConstants.SYNTAX_STYLE_NONE;
        };
        textArea.setSyntaxEditingStyle(style);
    }
}