package ru.app.draft.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class Notification implements Serializable {
     private String message;
     private String type;
     private String time;

    public Notification(String message, String type, String time) {
        this.message = message;
        this.type = type;
        this.time = time;
    }
}
