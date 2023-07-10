package ru.app.draft.models;

import java.io.Serializable;

public class MetricItem implements Serializable {

    private String method;
    //time
    private Long y;

    public MetricItem() {}

    public MetricItem(String method, Long y) {
        this.method = method;
        this.y = y;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Long getY() {
        return y;
    }

    public void setY(Long y) {
        this.y = y;
    }
}