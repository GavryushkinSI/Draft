package ru.app.draft.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@ToString
@Getter
@Setter
public class User implements Serializable {

    private String login;
    private String password;
    private String email;
    private String chatId;
    private String lastVisit;
    private Boolean isAdmin=false;

    public User(String login, String password, String email, String chatId) {
        this.login = login;
        this.password = password;
        this.email = email;
        this.chatId = chatId;
    }
}
