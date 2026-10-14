package com.editor.sync;

import java.io.Serializable;

public class DocumentSync implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ActionType {
        INSERT,
        DELETE,
        REPLACE_ALL,
        CURSOR_MOVE,
        CLIENT_DISCONNECT,
        JOIN_ROOM,
        AUTH_SUCCESS,
        AUTH_FAILED
    }

    private final ActionType actionType;
    private int offset;
    private int length;
    private String text;
    private String clientId;
    private int cursorPosition;
    private String roomId;
    private String roomPassword;

    public DocumentSync(ActionType actionType, int offset, int length, String text, String clientId) {
        this.actionType = actionType;
        this.offset = offset;
        this.length = length;
        this.text = text;
        this.clientId = clientId;
    }

    public DocumentSync(ActionType actionType, String clientId, int cursorPosition) {
        this.actionType = actionType;
        this.clientId = clientId;
        this.cursorPosition = cursorPosition;
    }

    public DocumentSync(ActionType actionType, String roomId, String roomPassword, String clientId) {
        this.actionType = actionType;
        this.roomId = roomId;
        this.roomPassword = roomPassword;
        this.clientId = clientId;
    }

    public ActionType getActionType() { return actionType; }
    public int getOffset() { return offset; }
    public int getLength() { return length; }
    public String getText() { return text; }
    public String getClientId() { return clientId; }
    public int getCursorPosition() { return cursorPosition; }
    public String getRoomId() { return roomId; }
    public String getRoomPassword() { return roomPassword; }
}