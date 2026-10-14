package com.editor.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;

import java.awt.Font;
import java.io.IOException;

public class SyntaxHighlighter {

    public static void applyDefaults(RSyntaxTextArea textArea) {
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setTabSize(4);

        try {
            Theme theme = Theme.load(
                SyntaxHighlighter.class.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml")
            );
            theme.apply(textArea);
        } catch (IOException | NullPointerException e) {
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        }
    }

    public static void setLanguage(RSyntaxTextArea textArea, String language) {
        switch (language.toLowerCase()) {
            case "python":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
                break;
            case "c++":
            case "cpp":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
                break;
            case "javascript":
            case "js":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                break;
            case "html":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
                break;
            case "json":
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                break;
            default:
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                break;
        }
    }
}