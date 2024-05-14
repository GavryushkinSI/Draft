package ru.app.draft.models;

import java.io.Serializable;

public enum EventLog implements Serializable {

    SIGNAL_FROM_TV,
    SET_CURRENT_POS_AFTER_EXECUTE,
    CORRECT_CURRENT_POS,
    QUANTITY_LESS_MIN_LOT,
    CANCEL_CONDITIONAL_ORDERS,
    CLOSE_OPEN_ORDERS_OR_REVERSE,
    MARKET_ORDER_EXECUTE,
}
