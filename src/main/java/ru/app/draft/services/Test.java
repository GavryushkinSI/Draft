package ru.app.draft.services;

import lombok.extern.log4j.Log4j2;
import ru.app.draft.models.CandleData;
import ru.app.draft.models.Message;
import ru.app.draft.models.Status;
import ru.app.draft.models.UserCache;
import ru.app.draft.store.Store;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.app.draft.store.Store.CANDLE_STORE;
import static ru.app.draft.store.Store.TEMP_STORE;

@Log4j2
public class Test {


    public static void main(String[] args) {
        CandleData candle1 = new CandleData("01-01-2022 00:00:00", List.of(1000L, 1500L, 900L, 1200L));
        unionCandle(candle1,"TEST");
        CandleData candle2 = new CandleData("01-01-2022 00:00:00", List.of(1000L, 2000L, 900L, 1200L));
        unionCandle(candle2,"TEST");
        CandleData candle3 = new CandleData("01-01-2022 00:00:00", List.of(1000L, 2000L, 900L, 1000L));
        unionCandle(candle3,"TEST");
        CandleData candle4 = new CandleData("01-01-2022 00:01:00", List.of(1000L, 2000L, 900L, 1000L));
        unionCandle(candle4,"TEST");
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
