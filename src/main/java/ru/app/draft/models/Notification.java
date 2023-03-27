package ru.app.draft.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.app.draft.utils.DateUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private String time = new Date().toString();
    private Boolean forAdmin;
    private Boolean blockCommentEnabled;
    private List<Comment> comments = new ArrayList<>();

    public Notification() {}

    public Notification(String header, String message, String type, String typeView, Boolean forAdmin) {
        this.header = header;
        this.message = message;
        this.type = type;
        this.typeView = typeView;
        this.forAdmin = forAdmin;
        this.id = UUID.randomUUID().toString();
    }

    public Notification(String header, String message, String type, String typeView, Boolean forAdmin, Boolean blockCommentEnabled) {
        this.header = header;
        this.message = message;
        this.type = type;
        this.typeView = typeView;
        this.forAdmin = forAdmin;
        this.id = UUID.randomUUID().toString();
        this.blockCommentEnabled = blockCommentEnabled;
    }

    public Notification(String header, String message, String type, String typeView, Boolean forAdmin, Boolean blockCommentEnabled, String time) {
        this.header = header;
        this.message = message;
        this.type = type;
        this.typeView = typeView;
        this.forAdmin = forAdmin;
        this.id = UUID.randomUUID().toString();
        this.blockCommentEnabled = blockCommentEnabled;
        this.time = time;
    }
}


