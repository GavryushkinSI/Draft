package ru.app.draft.models;

import com.google.protobuf.Timestamp;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class AccountDto implements Serializable {

    public AccountDto(String id, long cash) {
        this.id = id;
        this.cash = cash;
    }

    private String id;

    private long cash;

    private String figi;
    private Long lastPrice;
    private String lastTimeUpdate;
    private Long result;
    private List<String> logs;
    private List<Notification> notifications;
    private Boolean telegramSubscriptionExist = false;

    private long balance;
}
