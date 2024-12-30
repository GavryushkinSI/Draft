package ru.app.draft.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.tinkoff.piapi.contract.v1.OrderDirection;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
public class ConditionalOrder implements Serializable {

    private String orderLinkId;

    private String orderId;

    private String size;

    private OrderDirection direction;

    private int status;

    private Boolean canReplace;

    public ConditionalOrder(String orderId,String orderLinkId, String size, OrderDirection direction, Boolean canReplace) {
        this.orderId = orderId;
        this.size = size;
        this.direction = direction;
        this.orderLinkId=orderLinkId;
        this.canReplace=canReplace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionalOrder that = (ConditionalOrder) o;
        return status == that.status && Objects.equals(orderLinkId, that.orderLinkId) && Objects.equals(orderId, that.orderId) && Objects.equals(size, that.size) && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderLinkId, orderId, size, direction, status);
    }
}
