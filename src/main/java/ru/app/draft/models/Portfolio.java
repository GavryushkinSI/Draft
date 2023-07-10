package ru.app.draft.models;

import java.io.Serializable;

public class Portfolio implements Serializable {

    private String ticker;
    private Long lots;

    public Portfolio() {}

    public Portfolio(String ticker, Long lots) {
        this.ticker = ticker;
        this.lots = lots;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Long getLots() {
        return lots;
    }

    public void setLots(Long lots) {
        this.lots = lots;
    }
}
