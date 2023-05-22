package ru.app.draft.models;

import com.google.protobuf.Timestamp;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.tinkoff.piapi.contract.v1.PortfolioPosition;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@ToString
public class UserCache {
    private List<Strategy> strategies = new ArrayList<>();
    private List<Portfolio> portfolios = new ArrayList<>();
    private List<String> logs = new ArrayList<>();
    private Timestamp updateTime;
    private User user;
    private BigDecimal sumCommissions=BigDecimal.ZERO;

    public UserCache(User user) {
        this.user = user;
    }

    public void addLogs(String log) {
        logs.add(log);
    }

    public UserCache clearLog() {
        logs.clear();
        return this;
    }

    public void addPortfolio(List<PortfolioPosition> list) {
        portfolios.clear();
        list.forEach(i -> portfolios.add(new Portfolio(i.getFigi(), i.getQuantity().getUnits())));
    }

    public void addCommission(Long value){
        this.sumCommissions.add(BigDecimal.valueOf(value));
    }
}
