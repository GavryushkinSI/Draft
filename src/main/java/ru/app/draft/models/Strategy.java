package ru.app.draft.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class Strategy {
    private String id;
    private String userName;
    private String name;
    private String direction;
    private Long quantity;
    private String figi;
    private String ticker;
    private Boolean isActive;
    private List<String> consumer;
    private List<Order> orders;
    private Long currentPosition = 0L;
    private Long priceTv;

    public Strategy(String id, String userName, String name, String direction, Long quantity, String figi, String ticker, Boolean isActive, List<String> consumer, List<Order> orders) {
        this.id = id;
        this.userName = userName;
        this.name = name;
        this.direction = direction;
        this.quantity = quantity;
        this.figi = figi;
        this.ticker = ticker;
        this.isActive = isActive;
        this.consumer = consumer;
        this.orders = orders;
    }

    public void addOrder(Order order) {
        orders.add(order);
    }
}
