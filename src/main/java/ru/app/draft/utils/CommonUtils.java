package ru.app.draft.utils;

import ru.tinkoff.piapi.contract.v1.MoneyValue;

import java.math.BigDecimal;

public class CommonUtils {
    public static String formatNumber(BigDecimal value) {
        return value.scale() == 0 ? value.toString() : String.format("%.4f", value);
    }

    public static BigDecimal getFromMoneyValue(MoneyValue moneyValue){
        return moneyValue.getUnits() == 0 && moneyValue.getNano() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(moneyValue.getUnits()).add(BigDecimal.valueOf(moneyValue.getNano(), 9));
    }
}
