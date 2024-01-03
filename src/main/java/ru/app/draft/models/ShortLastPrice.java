package ru.app.draft.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
public class ShortLastPrice {
    private String figi;
    private BigDecimal price;
    private BigDecimal bid;
    private BigDecimal ask;
    private String updateTime;
}
