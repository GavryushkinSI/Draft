package ru.app.draft.utils;

import ru.tinkoff.piapi.contract.v1.MoneyValue;

import java.io.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

public class CommonUtils {
    private static final String FILE_LOG_NAME = "app.log";

    public static BigDecimal getBigDecimal(String value) {
        if(value==null){
            return null;
        }
        return BigDecimal.valueOf(Double.parseDouble(value));
    }

    public static String formatBigDecimalNumber(BigDecimal value) {
        // Целая часть числа
        int integerPart = (int) Math.abs(value.doubleValue());
        // Дробная часть числа
        double fractionalPart = Math.abs(Math.abs(value.doubleValue()) - integerPart);
        if (fractionalPart == 0) {
            return String.valueOf(integerPart);
        } else {
            return String.valueOf((integerPart + fractionalPart));
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

    public static Object parseResponse(Object res, String field) {
        if (res == null) {
            return null;
        }
        if (res instanceof LinkedHashMap) {
            LinkedHashMap<String, Object> resultMap = (LinkedHashMap<String, Object>) res;
            resultMap = (LinkedHashMap<String, Object>) resultMap.get("result");
            if (resultMap != null) {
                Object listObj=resultMap.get("list");
                if (listObj instanceof List) {
                    List<?> list = (List<?>) listObj;
                    if (!list.isEmpty() && list.get(0) instanceof LinkedHashMap) {
                        LinkedHashMap<String, Object> firstElement = (LinkedHashMap<String, Object>) list.get(0);
                        if(firstElement!=null){
                            return firstElement.get(field);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String countDecimalPlaces(String number) {
        if (number == null || number.isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be null or empty");
        }
        number = number.trim();

        int commaIndex = number.indexOf('.');

        // Если запятая не найдена, значит дробной части нет
        if (commaIndex == -1) {
            return String.valueOf(0);
        }

        // Получаем дробную часть
        String fractionalPart = number.substring(commaIndex + 1);

        // Возвращаем длину дробной части
        return String.valueOf(fractionalPart.length());
    }
}
