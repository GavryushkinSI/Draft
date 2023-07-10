package ru.app.draft.models;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class AccountDto implements Serializable {

    public AccountDto(String id, long cash) {
        this.id = id;
        this.cash = cash;
    }

    public AccountDto() {
    }

    private String id;

    private long cash;

    private String figi;

    private Long lastPrice;
    private String lastTimeUpdate;
    private Long result;
    private List<String> logs;
    private List<Notification> notifications;
    private List<Portfolio> portfolio=new ArrayList<>();
    private Set<String> viewedNotifyIds = new HashSet<>();
    private Boolean telegramSubscriptionExist = false;
    private long balance;

    public void addPortfolio(String ticker, Long lots){
        this.portfolio.add(new Portfolio(ticker, lots));
    }
}
