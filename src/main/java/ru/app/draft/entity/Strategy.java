package ru.app.draft.entity;

import javax.persistence.*;

@Entity
@Table(name = "strategy")
public class Strategy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "id_strategy")
    private Integer idStrategy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User users;

    @Column(name = "name", length = 20)
    private String name;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "figi")
    private String figi;

    @Column(name = "description")
    private String description;

    @Column(name = "\"position\"", nullable = false)
    private Integer position;

    @Column(name = "direction", length = 10)
    private String direction;

    @Column(name = "active", nullable = false)
    private Boolean active = false;

    @Column(name = "minlot")
    private Integer minLot;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getFigi() {
        return figi;
    }

    public void setFigi(String figi) {
        this.figi = figi;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUsers() {
        return users;
    }

    public void setUsers(User users) {
        this.users = users;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMinLot() {
        return minLot;
    }

    public void setMinLot(Integer minLot) {
        this.minLot = minLot;
    }

    public Integer getIdStrategy() {
        return idStrategy;
    }

    public void setIdStrategy(Integer idStrategy) {
        this.idStrategy = idStrategy;
    }
}