package ru.app.draft.utils;

import ru.tinkoff.piapi.contract.v1.MoneyValue;

import java.io.*;
import java.math.BigDecimal;

public class CommonUtils {
    private static final String FILE_LOG_NAME = "app.log";

    public static BigDecimal getBigDecimal(String value){
        return BigDecimal.valueOf(Double.parseDouble(value));
    }

    public static String formatBigDecimalNumber(BigDecimal value){
        // Целая часть числа
        int integerPart = (int) Math.abs(value.doubleValue());
        // Дробная часть числа
        double fractionalPart = Math.abs(Math.abs(value.doubleValue()) - integerPart);
        if (fractionalPart == 0) {
            return String.valueOf(integerPart);
        } else {
            return String.valueOf((integerPart+fractionalPart));
        }
    }

    public static String formatNumber(BigDecimal value, String priceScale) {
        return value.scale() == 0 ? value.toString() : String.format("%." + priceScale + "f", value);
    }

    public static BigDecimal getFromMoneyValue(MoneyValue moneyValue) {
        return moneyValue.getUnits() == 0 && moneyValue.getNano() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(moneyValue.getUnits()).add(BigDecimal.valueOf(moneyValue.getNano(), 9));
    }

    public static void clearLogFile() {
        try {
            FileWriter fw = new FileWriter(FILE_LOG_NAME);
            BufferedWriter bw = new BufferedWriter(fw);

            // Очистка файла, записывая в него пустую строку
            bw.write("");

            // Закрытие потоков
            bw.close();
            fw.close();
        } catch (IOException ignored) {
        }
    }

    public static String readLogFile(String filter) {
        try {
            FileReader fr = new FileReader(FILE_LOG_NAME);
            BufferedReader br = new BufferedReader(fr);

            StringBuilder logsText = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("MainController") || line.contains("ByBitService")) {
                    if (filter.equals("all")) {
                        logsText.append(line);
                        logsText.append("\n");
                    } else {
                        if (line.contains(filter)) {
                            logsText.append(line);
                            logsText.append("\n");
                        }
                    }
                }
            }

            // Закрытие потоков
            br.close();
            fr.close();

            return logsText.toString();
        } catch (IOException ignored) {
        }
        return null;
    }
}
