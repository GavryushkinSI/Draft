package ru.app.draft.entity;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "last_price")
public class LastPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name_subscriber", length = 20)
    private String nameSubscriber;

    @Column(name = "figi")
    private String figi;

    @Column(name = "price")
    private Integer price;

    @Column(name = "update_time")
    private Instant updateTime;

    public Instant getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getNameSubscriber() {
        return nameSubscriber;
    }

    public void setNameSubscriber(String nameSubscriber) {
        this.nameSubscriber = nameSubscriber;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }
}