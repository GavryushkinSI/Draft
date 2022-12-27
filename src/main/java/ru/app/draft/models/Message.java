package ru.app.draft.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class Message implements Serializable {
    private String senderName;
    private String receiverName;
    private Object message;
    private String date;
    private Status status;
    private String command;
}
