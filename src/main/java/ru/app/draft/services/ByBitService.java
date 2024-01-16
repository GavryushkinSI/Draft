package ru.app.draft.services;

import com.alibaba.fastjson.JSON;
import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.TradeOrderType;
import com.bybit.api.client.domain.TriggerBy;
import com.bybit.api.client.domain.account.AccountType;
import com.bybit.api.client.domain.account.request.AccountDataRequest;
import com.bybit.api.client.domain.institution.LendingDataRequest;
import com.bybit.api.client.domain.position.request.PositionDataRequest;
import com.bybit.api.client.domain.trade.Side;
import com.bybit.api.client.restApi.*;
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.security.HmacSHA256Signer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Timestamp;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.app.draft.exception.OrderNotExecutedException;
import ru.app.draft.models.*;
import org.springframework.stereotype.Service;
import ru.app.draft.utils.DateUtils;
import ru.tinkoff.piapi.contract.v1.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static com.bybit.api.client.constant.Util.generateTransferID;
import static ru.app.draft.store.Store.*;

@SuppressWarnings("ALL")
@Service
@Log4j2
public class ByBitService extends AbstractTradeService {

    private final TelegramBotService telegramBotService;
    private final MarketDataStreamService streamService;
    private final BybitApiTradeRestClient orderRestClient;
    private final BybitApiPositionRestClient positionRestClient;
    private final BybitApiMarketRestClient marketRestClient;
    private final BybitApiLendingRestClient lendingRestClient;

    private final BybitApiAccountRestClient accountClient;

    private final ObjectMapper mapper;

    private static final String PING_DATA = "{\"op\":\"ping\"}";

    public static Map<String, Strategy> orders = new HashMap<>();

    public ByBitService(TelegramBotService telegramBotService, MarketDataStreamService streamService, BybitApiTradeRestClient orderRestClient, BybitApiPositionRestClient positionRestClient, BybitApiMarketRestClient marketRestClient, BybitApiLendingRestClient lendingRestClient, BybitApiAccountRestClient accountClient, ObjectMapper mapper) {
        super(telegramBotService, streamService);
        this.telegramBotService = telegramBotService;
        this.streamService = streamService;
        this.orderRestClient = orderRestClient;
        this.positionRestClient = positionRestClient;
        this.marketRestClient = marketRestClient;
        this.lendingRestClient = lendingRestClient;
        this.accountClient = accountClient;
        this.mapper = mapper;
    }


    @Override
    public void sendSignal(Strategy strategy) {
        ErrorData errorData = null;
        Strategy changingStrategy = null;
        Map<String, Object> map = null;
        BigDecimal executionPrice = null;
        BigDecimal position = null;
        try {
            map = setCurrentPosition(strategy);
            if (map.size() == 0) {
                return;
            }
            changingStrategy = (Strategy) map.get("changingStrategy");
            if (!changingStrategy.getIsActive()) {
                return;
            }

            position = (BigDecimal) map.get("position");
            OrderDirection direction = (OrderDirection) map.get("direction");
            errorData = changingStrategy.getErrorData();
            if (changingStrategy.getConsumer().contains("terminal")) {
                var ordeId = (Optional<String>) map.get("orderId");
                var triggerPrice = (Optional<String>) map.get("triggerPrice");
                Map<String, Object> result = sendOrder(direction, position.toString(), changingStrategy.getTicker(), ordeId.isPresent()?ordeId.get():null, triggerPrice.isPresent()?triggerPrice.get():null);
                executionPrice = LAST_PRICE.get(changingStrategy.getFigi()).getPrice();
            } else if (changingStrategy.getConsumer().contains("test")) {
                executionPrice = LAST_PRICE.get(changingStrategy.getFigi()).getPrice();
            }
        } catch (Exception e) {
            if (changingStrategy == null) {
                UserCache userCache = USER_STORE.get(strategy.getUserName());
                List<Strategy> strategyList = userCache.getStrategies();
                changingStrategy = strategyList
                        .stream()
                        .filter(str -> str.getName().equals(strategy.getName())).findFirst().get();
            }
            errorData = new ErrorData();
            errorData.setMessage("Ошибка: " + e.getMessage());
            errorData.setTime(DateUtils.getCurrentTime());
            changingStrategy.setErrorData(errorData);
        }
        String time = DateUtils.getCurrentTime();
        List<Strategy> strategyList = (List<Strategy>) map.get("strategyList");
        UserCache userCache = (UserCache) map.get("userCache");
        updateStrategyCache(strategyList, strategy, changingStrategy, executionPrice, userCache, position, time);
    }

    private Map<String, Object> setCurrentPosition(Strategy strategy) {
        UserCache userCache = USER_STORE.get(strategy.getUserName());
        List<Strategy> strategyList = userCache.getStrategies();
        Strategy changingStrategy = strategyList
                .stream()
                .filter(str -> str.getName().equals(strategy.getName())).findFirst().get();

        if (strategy.getQuantity().compareTo(changingStrategy.getCurrentPosition()) == 0) {
            return new HashMap<>(0);
        }

        OrderDirection direction = null;
        if (strategy.getDirection().equals("buy")) {
            if (strategy.getQuantity().compareTo(changingStrategy.getCurrentPosition()) <= 0) {
                throw new OrderNotExecutedException(String.format("Неверный порядок ордеров, либо дублирование ордера: %s, %s!", "покупка", strategy.getQuantity()));
            }
            direction = OrderDirection.ORDER_DIRECTION_BUY;
        } else if (strategy.getDirection().equals("sell")) {
            if (strategy.getQuantity().compareTo(changingStrategy.getCurrentPosition()) >= 0) {
                throw new OrderNotExecutedException(String.format("Неверный порядок ордеров, либо дублирование ордера: %s, %s!", "продажа", strategy.getQuantity()));
            }
            direction = OrderDirection.ORDER_DIRECTION_SELL;
        } else {
            if (changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0) {
                direction = OrderDirection.ORDER_DIRECTION_BUY;
            } else {
                direction = OrderDirection.ORDER_DIRECTION_SELL;
            }
        }

        BigDecimal position = strategy.getQuantity().subtract(changingStrategy.getCurrentPosition());
        changingStrategy.setCurrentPosition(strategy.getQuantity());
        strategyList.set(Integer.parseInt(changingStrategy.getId()), changingStrategy);
        userCache.setStrategies(strategyList);
        USER_STORE.replace(strategy.getUserName(), userCache);

        if (strategy.getTriggerPrice() != null) {
            if (orders.containsKey(strategy.getOrderName())) {
                Strategy oldStrategy = orders.get(strategy.getOrderName());
                oldStrategy.setCurrentPosition(strategy.getQuantity());
                oldStrategy.setTriggerPrice(strategy.getTriggerPrice());
                orders.replace(strategy.getOrderName(), strategy);
            } else {
                var orderLinkedId = UUID.randomUUID().toString();
                strategy.setCurrentPosition(strategy.getQuantity());
                strategy.setOrderLinkedId(orderLinkedId);
                orders.put(strategy.getName(), strategy);
            }
        }

        Map<String, Object> map = ImmutableMap.of(
                "userCache", userCache,
                "strategyList", strategyList,
                "changingStrategy", changingStrategy,
                "position", position,
                "direction", direction,
                "orderId", Optional.ofNullable(strategy.getOrderLinkedId()),
                "triggerPrice", Optional.ofNullable(strategy.getTriggerPrice())
        );

        return map;
    }

    @Override
    Map<String, Object> getPositionInfo(String ticker) {
        var positionListRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).symbol(ticker).build();
        return (Map<String, Object>) positionRestClient.getPositionInfo(positionListRequest);
    }

    void getTickersInfo() {
        var insProductInfoRequest = LendingDataRequest.builder().build();
        var insProductInfo = lendingRestClient.getInsProductInfo(insProductInfoRequest);
    }

    Map<String, Object> getAccountInfo() {
        var walletBalanceRequest = AccountDataRequest.builder().accountType(AccountType.CONTRACT).coin("USDT").build();
        var walletBalanceData = accountClient.getWalletBalance(walletBalanceRequest);
        return (Map<String, Object>) walletBalanceData;
    }

    LinkedHashMap<String, Object> sendOrder(OrderDirection direction, String position, String ticker, String orderId, String triggerPrice) {
        TradeOrderRequest tradeOrderRequest = null;
        if (triggerPrice == null) {
            tradeOrderRequest = TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .symbol(ticker)
                    .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                    .orderType(TradeOrderType.MARKET)
                    .orderLinkId(orderId)
                    .qty(String.valueOf(Math.abs(Double.parseDouble(position))))
                    .build();
        } else {
            tradeOrderRequest = TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .symbol(ticker)
                    .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                    .orderType(TradeOrderType.MARKET)
                    .orderLinkId(orderId)
                    .triggerPrice(triggerPrice)
                    .triggerDirection(direction == OrderDirection.ORDER_DIRECTION_BUY ? 1 : 2)
                    .tpTriggerBy(TriggerBy.LAST_PRICE)
                    .qty(String.valueOf(Math.abs(Double.parseDouble(position))))
                    .build();
        }

        var response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
        if (!Objects.equal(response.get("retCode"), 0)) {
            throw new OrderNotExecutedException(String.format("Ошибка исполнения ордера %s, %s, %s", ticker, direction.name(), position));
        }
        return response;
    }

    public Ticker getFigi(List<String> tickers) {
        return TICKERS_BYBIT.get("tickers").stream().filter(i -> i.getValue().equals(tickers.get(0))).findFirst().get();
    }


    public void setStreamPublic() {
        Request request = new Request.Builder().url("wss://stream.bybit.com/v5/public/linear").build();
        OkHttpClient publicClient = new OkHttpClient.Builder().build();
        Map<String, Object> subscribeMsg = new LinkedHashMap<>();
        subscribeMsg.put("op", "subscribe");
        subscribeMsg.put("req_id", generateTransferID());
        subscribeMsg.put("args", List.of("tickers.BTCUSDT", "tickers.ETHUSDT"));
        WebSocket webSocket = publicClient.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                log.info(t.getMessage());
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                if (text != null) {
                    try {
                        Map<String, Object> result = (Map<String, Object>) JSON.parse(text);
                        String topic = (String) result.get("topic");
                        Timestamp time = getTimeStamp((Long) result.get("ts"));
                        if (topic.contains("tickers")) {
                            String ticker = topic.split("\\.")[1];
                            Map<String, Object> data = ((Map<String, Object>) result.get("data"));
                            //Object bid = data.get("bid1Price");
                            //Object ask = data.get("ask1Price");
                            BigDecimal lastPrice = BigDecimal.valueOf(Double.parseDouble((String) data.get("lastPrice")));
                            updateLastPrice(ticker, lastPrice, time);
                        }
                    } catch (Exception e) {
                        //log.info("error");
                    }
                }
            }

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                webSocket.send(JSON.toJSONString(subscribeMsg));
                log.info("success_bybit_public_stream");
            }

            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                setStreamPublic();
            }
        });
    }


    public void setStreamPrivate() {
        OkHttpClient privateClient = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url("wss://stream.bybit.com/v5/private?max_alive_time=1000m").build();
        Map<String, Object> subscribeMsg = new LinkedHashMap<>();
        subscribeMsg.clear();
        subscribeMsg.put("op", "subscribe");
        subscribeMsg.put("req_id", generateTransferID());
        subscribeMsg.put("args", List.of("execution"));
        WebSocket privateWebSocket = privateClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                try {
                    webSocket.send(createAuthMessage());
                    webSocket.send(JSON.toJSONString(subscribeMsg));
                } catch (Exception ex) {

                }
                log.info("success_bybit_private_stream");
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                super.onMessage(webSocket, text);
            }

            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                setStreamPrivate();
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                log.info(t.getMessage());
            }
        });
    }

    private Timestamp getTimeStamp(Long timeInMs) {
        long seconds = timeInMs / 1000;
        long nanos = (timeInMs % 1000) * 1000;
        return Timestamp.newBuilder().setSeconds(seconds).setNanos((int) nanos).build();
    }

    private String createAuthMessage() {
        long expires = Instant.now().toEpochMilli() + 10000;
        String val = "GET/realtime" + expires;
        String signature = HmacSHA256Signer.auth(val, "3t9kEGBf2hrOnz9zz4ukpVdYVJU9tiU2MaPv");

        var args = List.of("0SPHD7IM7JF4iNE5DK", expires, signature);
        var authMap = Map.of("req_id", generateTransferID(), "op", "auth", "args", args);
        return JSON.toJSONString(authMap);
    }

    public static Map<String, Strategy> getOrders() {
        return orders;
    }

    public static void setOrders(Map<String, Strategy> orders) {
        ByBitService.orders = orders;
    }
}
