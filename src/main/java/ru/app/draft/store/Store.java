package ru.app.draft.store;

import com.google.common.cache.CacheBuilder;
import com.google.protobuf.Timestamp;
import lombok.extern.log4j.Log4j2;
import ru.app.draft.annotations.Audit;
import ru.app.draft.models.*;
import ru.app.draft.services.MarketDataStreamService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

@Log4j2
public class Store {

    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public final static ConcurrentMap<String, CandleData> TEMP_STORE = CacheBuilder.newBuilder()
            .maximumSize(10)
            .<String, CandleData>build()
            .asMap();

    public final static ConcurrentMap<String, Set<CandleData>> CANDLE_STORE = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .<String, Set<CandleData>>build()
            .asMap();

    public final static ConcurrentMap<String, UserCache> USER_STORE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .<String, UserCache>build()
            .asMap();

    public final static ConcurrentMap<String, Strategy> STRATEGY_STORE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .<String, Strategy>build()
            .asMap();

    public final static ConcurrentMap<String, List<Notification>> COMMON_INFO = CacheBuilder.newBuilder()
            .maximumSize(100)
            .<String, List<Notification>>build()
            .asMap();

    public final static ConcurrentMap<String, LastPrice> LAST_PRICE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .<String, LastPrice>build()
            .asMap();

    public static void updateLastPrice(Long newValue, Timestamp time) {
        USER_STORE.computeIfPresent("Test", (s, data) -> {
            Map<String, Long> map = data.getMap();
            if (map.size() == 0) {
                map.put("RIH3", newValue);
            } else {
                map.replace("RIH3", newValue);
            }
            data.setMap(map);
            data.setUpdateTime(time);
            return data;
        });
    }

    public static void addCandle(String key, CandleData value) {
        CANDLE_STORE.computeIfAbsent(key, s -> {
            Set<CandleData> set = new TreeSet<>((o1, o2) -> {
                try {
                    return dateFormat.parse(o1.getX()).compareTo(dateFormat.parse(o2.getX()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            });
            set.add(value);
            return set;
        });
        CANDLE_STORE.computeIfPresent(key, (s, candleData) -> {
            candleData.add(value);
            return candleData;
        });
    }

    public static void changeUserInfo(Message message, MarketDataStreamService streamService) {
        String name = message.getSenderName();
        switch (message.getCommand()) {
            case "ADD_USER":
//                USER_STORE.computeIfAbsent(name, s -> new UserCache(name));
                break;
            case "SUBSCRIPTION_ON_TICKER":
                USER_STORE.computeIfPresent(name, (s, user) -> {
                    log.info(String.format("User:%s", user));
//                    user.setTicker((String) message.getMessage());
                    message.setSenderName("server");
                    message.setMessage(Store.CANDLE_STORE.get((String) message.getMessage()));
                    message.setStatus(Status.JOIN);
//                    streamService.sendDataToUser(Map.of(user.getUserName(), user), message);
                    return user;
                });
                break;
        }
    }
}
