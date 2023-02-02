package ru.app.draft.models;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AccountDto implements Serializable {

    public AccountDto(String id, long cash) {
        this.id = id;
        this.cash=cash;
    }

    private String id;

    private long cash;

    private String figi;
    private Long lastPrice;
    private Long result;
    private List<String> logs;

    private long balance;
}
