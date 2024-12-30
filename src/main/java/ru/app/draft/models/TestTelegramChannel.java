package ru.app.draft.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class TestTelegramChannel {

    private BigDecimal open;

    private BigDecimal profit;

    private BigDecimal quantity;

    private Long timeClose;

    private Integer countAdd=0;

    private BigDecimal closePrice;

    BigDecimal averageOpenPrice;

    private  int countFix=0;

    private Boolean fixByStop=false;

    private Boolean positionFix=false;

    private  BigDecimal pnl=null;

    private BigDecimal drawdown=null;
}
