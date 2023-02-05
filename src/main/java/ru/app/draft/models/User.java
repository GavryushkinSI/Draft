package ru.app.draft.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@ToString
@Getter
@Setter
@AllArgsConstructor
public class User implements Serializable {

    private String login;
    private String password;
    private String email;
}
