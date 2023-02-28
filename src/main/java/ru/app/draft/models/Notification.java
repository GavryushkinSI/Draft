package ru.app.draft.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@ToString
public class Notification implements Serializable {
    private String id;
    private String message;
    private String header;
    private String type;
    private String typeView;
    private String time;
    private Boolean forAdmin;


    public Notification(String header, String message, String type, String typeView, String time, Boolean forAdmin) {
        this.header=header;
        this.message = message;
        this.type = type;
        this.typeView = typeView;
        this.time = time;
        this.forAdmin = forAdmin;
        this.id = UUID.randomUUID().toString();
    }
}
