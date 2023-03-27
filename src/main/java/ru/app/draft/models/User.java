package ru.app.draft.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private Set<String> viewedNotifyIds= new HashSet<>();

    public User() {}

    public User(String login, String password, String email, String chatId) {
        this.login = login;
        this.password = password;
        this.email = email;
        this.chatId = chatId;
    }

    public void addViewedNotifyIds(List<String> ids){
        viewedNotifyIds.addAll(ids);
    }
}
