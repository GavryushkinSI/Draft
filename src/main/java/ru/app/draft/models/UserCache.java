package ru.app.draft.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserCache {
    private String userName;
    private String ticker;

    public UserCache(String userName) {
        this.userName = userName;
    }
}
