package com.editor.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteCaretPainter {

    public static class PeerCursor {
        public final String name;
        public final Color color;
        public int position;

        public PeerCursor(String name, Color color, int position) {
            this.name = name;
            this.color = color;
            this.position = position;
        }
    }

    private static final Color[] PEER_COLORS = {
        new Color(231, 76, 60),   // Coral Red
        new Color(46, 204, 113),  // Emerald Green
        new Color(52, 152, 219),  // Sky Blue
        new Color(155, 89, 182),  // Purple
        new Color(241, 196, 15)   // Sun Yellow
    };

    private final RSyntaxTextArea textArea;
    private final Map<String, PeerCursor> peerCursors = new ConcurrentHashMap<>();

    public RemoteCaretPainter(RSyntaxTextArea textArea) {
        this.textArea = textArea;
    }

    public void updatePeerCursor(String clientId, int position) {
        peerCursors.compute(clientId, (id, existing) -> {
            if (existing != null) {
                existing.position = Math.min(position, textArea.getDocument().getLength());
                return existing;
            }
            int colorIndex = Math.abs(id.hashCode()) % PEER_COLORS.length;
            return new PeerCursor(id, PEER_COLORS[colorIndex], Math.min(position, textArea.getDocument().getLength()));
        });
        textArea.repaint();
    }

    public void removePeer(String clientId) {
        peerCursors.remove(clientId);
        textArea.repaint();
    }

    public void paintRemoteCarets(Graphics2D g2) {
        int docLength = textArea.getDocument().getLength();

        for (PeerCursor cursor : peerCursors.values()) {
            int pos = Math.min(cursor.position, docLength);
            try {
                Rectangle2D rect = textArea.modelToView2D(pos);
                if (rect == null) continue;

                int x = (int) rect.getX();
                int y = (int) rect.getY();
                int h = (int) rect.getHeight();

                // Draw vertical caret line
                g2.setColor(cursor.color);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(x, y, x, y + h);

                // Draw name tag badge
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                int badgeW = fm.stringWidth(cursor.name) + 6;
                int badgeH = fm.getHeight();

                g2.fillRect(x, Math.max(0, y - badgeH), badgeW, badgeH);
                g2.setColor(Color.WHITE);
                g2.drawString(cursor.name, x + 3, Math.max(badgeH - 3, y - 3));

            } catch (BadLocationException ignored) {}
        }
    }
}