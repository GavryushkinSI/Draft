package ru.app.draft.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
    private BigDecimal quantity;
    private String figi;
    private String ticker;
    private Boolean isActive;
    private List<String> consumer;
    private List<Order> orders;
    private BigDecimal currentPosition = BigDecimal.ZERO;
    //private List<ConditionalOrder> conditionalOrders = new ArrayList<>();
    private Long priceTv;
    private String producer;
    @JsonIgnoreProperties
    private String position;
    @JsonIgnoreProperties
    private String slippage;
    @JsonIgnoreProperties
    private BigDecimal minLot;
    private String description;
    private BigDecimal triggerPrice;
    private String orderName;
    private String orderLinkedId;
    private ErrorData errorData = new ErrorData();
    private List<String> enterAveragePrice = new ArrayList<>();
    private String comment;
    private Boolean isExecution = false;
    private StrategyOptions options=new StrategyOptions();
    private Boolean doReverse;
    private String priceScale;
    private BigDecimal positionTv;
    private Long createdDate;

    public Strategy() {
    }

    public Strategy(String id, String userName, String name, String direction, BigDecimal quantity, String figi, String ticker, Boolean isActive, List<String> consumer, List<Order> orders, String description, BigDecimal minLot, String producer) {
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
        this.producer = producer;
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public void addEnterAveragePrice(BigDecimal price, boolean beginClear){
        if (beginClear) {
            this.enterAveragePrice.clear();
        }else {
            this.enterAveragePrice.add(String.valueOf(price));
        }
    }
}
