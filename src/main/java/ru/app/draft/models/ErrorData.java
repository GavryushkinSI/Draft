package ru.app.draft.models;

import java.io.Serializable;

public class ErrorData implements Serializable {

    private String message;
    private String time;

    public ErrorData(String message) {
        this.message = message;
    }
    public ErrorData(String message, String time) {
        this.message = message;
        this.time = time;
    }

    public ErrorData() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
