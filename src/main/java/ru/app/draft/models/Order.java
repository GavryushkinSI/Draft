package ru.app.draft.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class Order {
    private BigDecimal price;

    public Order() {}

    public Order(BigDecimal price, BigDecimal quantity, String direction, String date) {
        this.price = price;
        this.quantity = quantity;
        this.direction = direction;
        this.date = date;
    }

    private BigDecimal quantity;
    private String direction;
    private String date;
}
