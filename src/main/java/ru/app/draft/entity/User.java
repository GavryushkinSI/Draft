package ru.app.draft.entity;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "login", nullable = false, length = 20)
    private String login;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "chartid")
    private String chartid;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "last_visit")
    private Instant lastVisit;

    @OneToMany(orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private List<Strategy> strategies = new ArrayList<>();

    public List<Strategy> getStrategies() {
        return strategies;
    }

    public void setStrategies(List<Strategy> strategies) {
        this.strategies = strategies;
    }

    public Instant getLastVisit() {
        return lastVisit;
    }

    public void setLastVisit(Instant lastVisit) {
        this.lastVisit = lastVisit;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getChartid() {
        return chartid;
    }

    public void setChartid(String chartid) {
        this.chartid = chartid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    //TODO Reverse Engineering! Migrate other columns to the entity
}