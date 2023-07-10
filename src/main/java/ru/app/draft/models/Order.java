package ru.app.draft.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class Order {
    private BigDecimal price;

    public Order() {}

    public Order(BigDecimal price, Long quantity, String direction, String date) {
        this.price = price;
        this.quantity = quantity;
        this.direction = direction;
        this.date = date;
    }

    private Long quantity;
    private String direction;
    private String date;
}
