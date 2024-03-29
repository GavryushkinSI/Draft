package ru.app.draft.services;

import jdk.net.Sockets;
import lombok.extern.log4j.Log4j2;
import ru.app.draft.models.CandleData;
import ru.app.draft.models.Message;
import ru.app.draft.models.Status;
import ru.app.draft.models.UserCache;
import ru.app.draft.store.Store;
import ru.app.draft.utils.DateUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.app.draft.store.Store.CANDLE_STORE;
import static ru.app.draft.store.Store.TEMP_STORE;

@Log4j2
public class Test {
    static boolean letterOfRegInterBankTransfersServicesEnable =false;
    static boolean payrollCreditPaymentServiceEnable = false;
    static String ADMISSION_VALUE_FOR_SELF_EMPLOYED = "87";

    public static void main(String[] args) {
        test();
    }

    public static boolean test(){
        if ((letterOfRegInterBankTransfersServicesEnable || payrollCreditPaymentServiceEnable)
                //Идём в легаси, если договор относиться к типу Самозанятые
                || Objects.equals(null, ADMISSION_VALUE_FOR_SELF_EMPLOYED)) {

            return false;
        }
        return true;
    }

    private static void unionCandle(CandleData candle, String figi) {
        if (TEMP_STORE.size() == 0) {
            TEMP_STORE.put(figi, candle);
            return;
        }
        if (TEMP_STORE.containsValue(candle)) {
            TEMP_STORE.compute("TEST", (s, data) -> {
                List<Long> currentPrice = new ArrayList<>(data.getY());
                if (candle.getY().get(1) > currentPrice.get(1)) {
                    currentPrice.set(1, candle.getY().get(1));
                }
                if (candle.getY().get(2) < currentPrice.get(2)) {
                    currentPrice.set(2, candle.getY().get(2));
                }
                currentPrice.set(3, candle.getY().get(3));
                data.setY(currentPrice);
                return data;
            });
        } else {
            CandleData newCandle = TEMP_STORE.get("TEST");
            CANDLE_STORE.computeIfPresent("TEST", (s, data) -> {
                data.add(newCandle);
                return data;
            });
            TEMP_STORE.clear();
            TEMP_STORE.put("TEST", candle);
        }
    }
}
