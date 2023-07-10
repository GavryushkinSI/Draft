package ru.app.draft.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.util.Pair;
import ru.app.draft.utils.PairDeserializer;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class Strategy implements Serializable {
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
    @JsonIgnoreProperties
    private String producer;
    @JsonIgnoreProperties
    private String position;
    @JsonIgnoreProperties
    private String slippage;
    @JsonIgnoreProperties
    private Long minLot;
    private String description;
    private ErrorData errorData = new ErrorData();
    private List<String> enterAveragePrice = new ArrayList<>();

    public Strategy() {
    }

    public Strategy(String id, String userName, String name, String direction, Long quantity, String figi, String ticker, Boolean isActive, List<String> consumer, List<Order> orders, String description, Long minLot) {
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
        this.description = description;
        this.minLot = minLot;
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public void addEnterAveragePrice(BigDecimal price, boolean beginClear){
        if (beginClear) {
            this.enterAveragePrice.clear();
        }
        this.enterAveragePrice.add(String.valueOf(price));
    }
}
