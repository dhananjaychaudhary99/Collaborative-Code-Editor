package com.editor.sync;

import java.io.Serializable;

public class DocumentSync implements Serializable {
    private static final long serialVersionUID = 2L;

    public enum ActionType {
        INSERT,
        DELETE,
        REPLACE_ALL,
        CURSOR_MOVE,
        CLIENT_DISCONNECT
    }

    private final ActionType actionType;
    private final int offset;
    private final int length;
    private final String text;
    private final String clientId;
    private final int cursorPosition;

    // Document edit constructor
    public DocumentSync(ActionType actionType, int offset, int length, String text, String clientId) {
        this.actionType = actionType;
        this.offset = offset;
        this.length = length;
        this.text = text != null ? text : "";
        this.clientId = clientId;
        this.cursorPosition = offset;
    }

    // Cursor position / disconnect constructor
    public DocumentSync(ActionType actionType, String clientId, int cursorPosition) {
        this.actionType = actionType;
        this.offset = cursorPosition;
        this.length = 0;
        this.text = "";
        this.clientId = clientId;
        this.cursorPosition = cursorPosition;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public String getText() {
        return text;
    }

    public String getClientId() {
        return clientId;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }
}