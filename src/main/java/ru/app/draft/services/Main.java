package ru.app.draft.services;

import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        BigDecimal val = new BigDecimal("0.00100");
        // Целая часть числа
        long integerPart = val.longValue();

        // Дробная часть числа
        double fractionalPart = val.doubleValue() - integerPart;
        if (fractionalPart == 0) {
            System.out.println(BigDecimal.valueOf(integerPart));
        } else {
            System.out.println(BigDecimal.valueOf(integerPart+fractionalPart));
        }
    }
}
