package ru.app.draft.models;

import java.io.Serializable;

public class ErrorData implements Serializable {
    private String message;

    public ErrorData(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
