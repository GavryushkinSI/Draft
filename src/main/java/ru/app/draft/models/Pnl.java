package ru.app.draft.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
public class Pnl implements Serializable {

    String symbol;
    BigDecimal closedPnl;
    String orderId;
    Long time;
    BigDecimal fee;
    String size;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pnl pnl = (Pnl) o;
        return Objects.equals(symbol, pnl.symbol) && Objects.equals(closedPnl, pnl.closedPnl) && Objects.equals(orderId, pnl.orderId) && Objects.equals(time, pnl.time) && Objects.equals(fee, pnl.fee) && Objects.equals(size, pnl.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, closedPnl, orderId, time, fee, size);
    }
}
