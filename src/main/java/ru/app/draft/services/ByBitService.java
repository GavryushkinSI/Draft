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
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.restApi.BybitApiAccountRestClient;
import com.bybit.api.client.restApi.BybitApiLendingRestClient;
import com.bybit.api.client.restApi.BybitApiMarketRestClient;
import com.bybit.api.client.restApi.BybitApiPositionRestClient;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.bybit.api.client.security.HmacSHA256Signer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Timestamp;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.CollectionUtils;
import ru.app.draft.exception.OrderNotExecutedException;
import ru.app.draft.models.*;
import org.springframework.stereotype.Service;
import ru.app.draft.utils.DateUtils;
import ru.tinkoff.piapi.contract.v1.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.bybit.api.client.constant.Util.generateTransferID;
import static ru.app.draft.store.Store.LAST_PRICE;
import static ru.app.draft.store.Store.ORDERS_MAP;
import static ru.app.draft.store.Store.TICKERS_BYBIT;
import static ru.app.draft.store.Store.USER_STORE;
import static ru.app.draft.store.Store.modifyOrdersMap;
import static ru.app.draft.store.Store.updateLastPrice;

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
        UserCache userCache = null;
        List<Strategy> strategyList = null;
        try {
            map = setCurrentPosition(strategy, null, null, null, null, null, null, null);

            if (map != null) {
                userCache = USER_STORE.get(strategy.getUserName());
                strategyList = userCache.getStrategies();
            } else {
                return;
            }

            changingStrategy = (Strategy) map.get("changingStrategy");
            if (!changingStrategy.getIsActive()) {
                return;
            }

            position = (BigDecimal) map.get("position");
            OrderDirection direction = (OrderDirection) map.get("direction");
            if (changingStrategy.getConsumer().contains("terminal")) {
                var ordeId = (String) map.get("orderId");
                var triggerPrice = (Optional<BigDecimal>) map.get("triggerPrice");
                var isAmendOrder = (Boolean) map.get("isAmendOrder");
                Map<String, Object> result = sendOrder(direction, position.toString(), changingStrategy.getTicker(), ordeId, triggerPrice.isPresent() ? String.valueOf(triggerPrice.get()) : null, isAmendOrder, changingStrategy.getOptions(), LAST_PRICE.get(changingStrategy.getFigi()).getPrice(), changingStrategy.getName(), strategy.getComment());
                executionPrice = LAST_PRICE.get(changingStrategy.getFigi()).getPrice();
            } else if (changingStrategy.getConsumer().contains("test")) {
                executionPrice = LAST_PRICE.get(changingStrategy.getFigi()).getPrice();
            }
        } catch (Exception e) {
            if (changingStrategy == null) {
                userCache = USER_STORE.get(strategy.getUserName());
                strategyList = userCache.getStrategies();
                changingStrategy = strategyList
                        .stream()
                        .filter(str -> str.getName().equals(strategy.getName())).findFirst().get();
            }
            errorData = new ErrorData();
            errorData.setMessage("Ошибка: " + e.getMessage());
            errorData.setTime(DateUtils.getCurrentTime());
            changingStrategy.setErrorData(errorData);
        }
        if (errorData != null) {
            sendMessageInSocket(strategyList);
        }
        if (changingStrategy.getConsumer().contains("test")) {
            String time = DateUtils.getCurrentTime();
            updateStrategyCache(strategyList, strategy, changingStrategy, executionPrice, userCache, position, time, false);
        }
    }

    public synchronized Map<String, Object> setCurrentPosition(@Nullable Strategy strategy, String orderLinkedId, BigDecimal executionPrice, BigDecimal lastPrice, String side, BigDecimal execQty, BigDecimal currentPosition, String symbol) {
        UserCache userCache = USER_STORE.get("Admin");
        List<Strategy> strategyList = userCache.getStrategies();

        if (orderLinkedId != null || symbol != null) {
            Strategy execStrategy = strategyList
                    .stream()
                    .filter(item -> item.getFigi().equals(symbol)).findFirst().get();
            executionPrice = LAST_PRICE.get(execStrategy.getFigi()).getPrice();
            if (Objects.equal(currentPosition.doubleValue(), execStrategy.getCurrentPosition().doubleValue())) {
                return null;
            }
            if (execStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) == 0) {
                execStrategy.setDirection(side.toLowerCase());
            }
            if (side.toLowerCase().equals("buy") && currentPosition.compareTo(execStrategy.getCurrentPosition()) > 0) {
                execStrategy.setDirection("buy");
            }
            if (side.toLowerCase().equals("buy") && currentPosition.compareTo(execStrategy.getCurrentPosition()) < 0) {
                execStrategy.setDirection("sell");
            }
            if (side.toLowerCase().equals("sell") && currentPosition.doubleValue() > Math.abs(execStrategy.getCurrentPosition().doubleValue())) {
                execStrategy.setDirection("sell");
            }
            if (side.toLowerCase().equals("sell") && currentPosition.doubleValue() < Math.abs(execStrategy.getCurrentPosition().doubleValue())) {
                execStrategy.setDirection("buy");
            }
            execQty = BigDecimal.valueOf(Math.abs(Math.abs(execStrategy.getCurrentPosition().doubleValue()) - currentPosition.doubleValue()));
            if (execQty.compareTo(BigDecimal.ZERO) < 0) {
                execQty = execQty.multiply(BigDecimal.valueOf(-1.0d));
            }
            updateStrategyCache(strategyList, execStrategy, execStrategy, executionPrice, userCache, execQty, DateUtils.getCurrentTime(), false);
//            ORDERS_MAP.forEach((k, v) -> {
//                if (v.contains(orderLinkedId)) {
//                    Strategy execStrategy = strategyList
//                            .stream()
//                            .filter(item -> item.getName().equals(k)).findFirst().get();
//                    execStrategy.setDirection(side.toLowerCase());
//                    updateStrategyCache(strategyList, execStrategy, execStrategy, executionPrice, userCache, execQty, DateUtils.getCurrentTime(), false);
//                }
//            });
            return null;
        }
        if (strategy == null) {
            return null;
        }

//        if (orderLinkedId != null || lastPrice != null) {
//            ORDERS_MAP.entrySet().stream()
//                    .forEach(entry -> {
//                        Strategy str = entry.getValue();
//                        if (!str.getIsExecution()) {
//                            if (lastPrice == null) {
//                                if (str.getOrderLinkedId().equals(orderLinkedId)) {
//                                    Strategy changingStrategyFromCond = strategyList
//                                            .stream()
//                                            .filter(i -> i.getName().equals(str.getName())).findFirst().get();
//                                    BigDecimal position = str.getQuantity().subtract(changingStrategyFromCond.getCurrentPosition());
//                                    changingStrategyFromCond.setCurrentPosition(str.getQuantity());
//                                    strategyList.set(Integer.parseInt(changingStrategyFromCond.getId()), changingStrategyFromCond);
//                                    userCache.setStrategies(strategyList);
//                                    USER_STORE.replace(str.getUserName(), userCache);
//                                    String time = DateUtils.getCurrentTime();
//                                    updateStrategyCache(strategyList, str, changingStrategyFromCond, executionPrice, userCache, position, time);
//                                    str.setIsExecution(true);
//                                    entry.setValue(str);
//                                }
//                            } else {
//                                String direct = str.getDirection();
//                                BigDecimal trPrice = str.getTriggerPrice();
//                                if ((direct.equals("buy") && trPrice.compareTo(lastPrice) <= 0)
//                                        || (direct.equals("sell") && trPrice.compareTo(lastPrice) >= 0)
//                                ) {
//                                    Strategy changingStrategyFromCond = strategyList
//                                            .stream()
//                                            .filter(i -> i.getName().equals(str.getName())).findFirst().get();
//
//                                    BigDecimal position = str.getQuantity().subtract(changingStrategyFromCond.getCurrentPosition());
//                                    changingStrategyFromCond.setCurrentPosition(str.getQuantity());
//                                    strategyList.set(Integer.parseInt(changingStrategyFromCond.getId()), changingStrategyFromCond);
//                                    userCache.setStrategies(strategyList);
//                                    USER_STORE.replace(str.getUserName(), userCache);
//                                    String time = DateUtils.getCurrentTime();
//                                    updateStrategyCache(strategyList, str, changingStrategyFromCond, lastPrice, userCache, position, time);
//                                    str.setIsExecution(true);
//                                    entry.setValue(str);
//                                }
//                            }
//                        }
//                    });
//            return null;
//        }

        Strategy changingStrategy = strategyList
                .stream()
                .filter(item -> item.getName().equals(strategy.getName())).findFirst().get();

        if (strategy.getQuantity().compareTo(BigDecimal.ZERO) != 0 &&
                Math.abs(strategy.getQuantity().doubleValue()) < 0.001d) {
            return null;
        }

        if (strategy.getQuantity().compareTo(changingStrategy.getCurrentPosition()) == 0) {
            return null;
        }

        OrderDirection direction = null;
        if (strategy.getDirection().equals("buy")) {
            if (strategy.getTriggerPrice() == null && strategy.getQuantity().compareTo(changingStrategy.getCurrentPosition()) <= 0) {
                throw new OrderNotExecutedException(String.format("Неверный порядок ордеров, либо дублирование ордера: %s, %s!", "покупка", strategy.getQuantity()));
            }
            direction = OrderDirection.ORDER_DIRECTION_BUY;
        } else if (strategy.getTriggerPrice() == null && strategy.getDirection().equals("sell")) {
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


//        boolean isAmendOrder = false;
//        Strategy replacingStrategy = ORDERS_MAP.get(strategy.getOrderName());
//        String newOrderLinkedId = UUID.randomUUID().toString();
//        boolean isExecutStrategy = replacingStrategy != null ? replacingStrategy.getIsExecution() != true : true;
//        boolean triggerPriceNotNull = strategy.getTriggerPrice() != null;
//        boolean removeExceStatus = false;
//        if (triggerPriceNotNull) {
//            if (strategy.getQuantity().compareTo(BigDecimal.ZERO) > 0 && changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0
//                    || strategy.getQuantity().compareTo(BigDecimal.ZERO) < 0 && changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0
//                    || strategy.getQuantity().compareTo(BigDecimal.ZERO) < 0 && changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) > 0
//                    || strategy.getQuantity().compareTo(BigDecimal.ZERO) > 0 && changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) > 0
//            ) {
//                removeExceStatus = true;
//            }
//        }
//        if (triggerPriceNotNull) {
//            if (ORDERS_MAP.containsKey(strategy.getOrderName())) {
//                if (isExecutStrategy && removeExceStatus || !isExecutStrategy) {
//                    //if order execute open new order
//                    if (isExecutStrategy && removeExceStatus) {
//                        newOrderLinkedId = UUID.randomUUID().toString();
//                        replacingStrategy.setOrderLinkedId(newOrderLinkedId);
//                    } else {
//                        //amend order
//                        newOrderLinkedId = replacingStrategy.getOrderLinkedId();
//                        isAmendOrder = true;
//                    }
//                    replacingStrategy.setIsExecution(false);
//                    replacingStrategy.setQuantity(strategy.getQuantity());
//                    replacingStrategy.setTriggerPrice(strategy.getTriggerPrice());
//                    replacingStrategy.setDirection(strategy.getDirection());
//                    ORDERS_MAP.replace(strategy.getOrderName(), replacingStrategy);
//                }
//            } else {
//                newOrderLinkedId = UUID.randomUUID().toString();
//                strategy.setCurrentPosition(strategy.getQuantity());
//                strategy.setOrderLinkedId(newOrderLinkedId);
//                ORDERS_MAP.put(strategy.getOrderName(), strategy);
//            }
//        }

        BigDecimal position = strategy.getQuantity().subtract(changingStrategy.getCurrentPosition());
//        if (triggerPriceNotNull != true) {
//            changingStrategy.setCurrentPosition(strategy.getQuantity());
//        }
        strategyList.set(Integer.parseInt(changingStrategy.getId()), changingStrategy);
        userCache.setStrategies(strategyList);
        USER_STORE.replace(strategy.getUserName(), userCache);

        Map<String, Object> map = ImmutableMap.of(
                "userCache", userCache,
                "strategyList", strategyList,
                "changingStrategy", changingStrategy,
                "position", position,
                "direction", direction,
                "orderId", orderLinkedId == null ? UUID.randomUUID().toString() : orderLinkedId,
                "isAmendOrder", false,
                "triggerPrice", Optional.ofNullable(strategy.getTriggerPrice())
        );

        return map;
    }

    @Override
    public Map<String, Object> getPositionInfo() {
        var positionListRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).settleCoin("USDT").build();
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

    LinkedHashMap<String, Object> sendOrder(OrderDirection direction, String position, String ticker, String orderId, String triggerPrice, Boolean isAmendOrder, StrategyOptions options, BigDecimal executionPrice, String name, String comment) {
        TradeOrderRequest tradeOrderRequest = null;
        LinkedHashMap<String, Object> response = null;
//        List<String> list = ORDERS_MAP.get(name);
//        if (CollectionUtils.isEmpty(list)) {
//            list = new ArrayList<>();
//            ORDERS_MAP.put(name, list);
//        } else {
//            list.clear();
//        }
//        modifyOrdersMap(orderId, name);
        cancelOrders(ticker);
        if (triggerPrice == null && options.getUseGrid() && comment != null && comment.contains("grid")) {
            tradeOrderRequest = TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .symbol(ticker)
                    .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                    .orderType(TradeOrderType.MARKET)
                    .orderLinkId(orderId)
                    .qty(String.valueOf(Math.abs(Double.parseDouble(position))))
                    .build();
            response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
            var retCode3 = response.get("retCode");
            if (!Objects.equal(retCode3, 0)) {
                var message = response.get("retMsg");
                throw new OrderNotExecutedException(String.format("Ошибка исполнения рыночного ордера. Message: %s.", message));
            }

            gridOrders(direction, ticker, options, executionPrice);
        }
        if (comment == null || !comment.contains("grid")) {
            if (triggerPrice == null) {
                tradeOrderRequest = TradeOrderRequest.builder()
                        .category(CategoryType.LINEAR)
                        .symbol(ticker)
                        .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                        .orderType(TradeOrderType.MARKET)
                        .orderLinkId(orderId)
                        .qty(String.valueOf(Math.abs(Double.parseDouble(position))))
                        .build();
                response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
            } else {
                if (isAmendOrder) {
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
                    response = (LinkedHashMap<String, Object>) orderRestClient.amendOrder(tradeOrderRequest);
                } else {
                    //trigerPrice!=null
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
                    response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);

                    if (options.getUseGrid() && comment != null && comment.contains("grid")) {
                        gridOrders(direction, ticker, options, executionPrice);
                    }
                }
            }
        }

        var retCode = response.get("retCode");
        if (!Objects.equal(retCode, 0) && !Objects.equal(retCode, 110092) && !Objects.equal(retCode, 110093)) {
            var message = response.get("retMsg");
            throw new OrderNotExecutedException(String.format("Ошибка исполнения ордера %s, %s, %s. Message: %s.", ticker, direction.name(), position, message));
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
        subscribeMsg.put("args", List.of("tickers.BTCUSDT", "tickers.ETHUSDT", "tickers.APTUSDT", "tickers.ORDIUSDT",
                "tickers.FETUSDT", "tickers.XRPUSDT", "tickers.AVAXUSDT", "tickers.GRTUSDT",
                "tickers.ARBUSDT", "tickers.DOTUSDT"
        ));

        WebSocket webSocket = publicClient.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                try {
                    Thread.sleep(5000);
                    setErrorAndSetOnUi(String.format("WebSocket failure reason: %s", t.getMessage()));
                } catch (Exception e) {
                }
                setStreamPublic();
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                if (text != null) {
                    try {
                        Map<String, Object> result = (Map<String, Object>) JSON.parse(text);
                        String topic = (String) result.get("topic");
                        if (topic != null) {
                            Timestamp time = getTimeStamp((Long) result.get("ts"));
                            if (topic.contains("tickers")) {
                                String ticker = topic.split("\\.")[1];
                                Map<String, Object> data = ((Map<String, Object>) result.get("data"));
                                BigDecimal lastPrice = BigDecimal.valueOf(Double.parseDouble((String) data.get("lastPrice")));
                                updateLastPrice(ticker, lastPrice, time);
//                                if (ORDERS_MAP.size() != 0) {
//                                    setCurrentPosition(null, null, null, lastPrice);
//                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                Thread pingThread = createPingThread(webSocket);
                pingThread.setName("thread-public-ping");
                pingThread.start();
                webSocket.send(JSON.toJSONString(subscribeMsg));
                log.info("success_bybit_public_stream");
            }

            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                try {
                    Thread.sleep(5000);
                    setErrorAndSetOnUi(String.format("WebSocket closed reason: %s", reason));
                } catch (Exception e) {

                }
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
        //subscribeMsg.put("args", List.of("execution"));
        subscribeMsg.put("args", List.of("position.linear"));
        WebSocket privateWebSocket = privateClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                Thread pingThread = createPingThread(webSocket);
                pingThread.setName("thread-private-ping");
                pingThread.start();
                webSocket.send(createAuthMessage());
                webSocket.send(JSON.toJSONString(subscribeMsg));
                log.info("success_bybit_private_stream");
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                if (text != null) {
                    try {
                        Map<String, Object> result = (Map<String, Object>) JSON.parse(text);
                        String topic = (String) result.get("topic");
                        if (topic != null && topic.equals("position.linear")) {
                            Map<String, Object> map = ((Map<String, Object>) ((List<Object>) result.get("data")).get(0));
                            BigDecimal size = BigDecimal.valueOf(Double.parseDouble((String) map.get("size")));
                            String side = (String) map.get("side");
                            String symbol = (String) map.get("symbol");
                            setCurrentPosition(null, null, null, null, side, null, size, symbol);
                        }
//                        String topic = (String) result.get("topic");
//                        Map<String, Object> map = ((Map<String, Object>) ((List<Object>) result.get("data")).get(0));
//                        BigDecimal execPrice = BigDecimal.valueOf(Double.parseDouble((String) map.get("execPrice")));
//                        String side = (String) map.get("side");
//                        BigDecimal execQty = BigDecimal.valueOf(Double.parseDouble((String) map.get("execQty")));
//                        String orderLinkId = (String) map.get("orderLinkId");
//                        setCurrentPosition(null, orderLinkId, execPrice, null, side, execQty);
                    } catch (Exception ex) {
                        setErrorAndSetOnUi(String.format("WebSocket failure reason onMesagePrivate: %s", ex.getMessage()));
                    }
                }
            }

            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                try {
                    Thread.sleep(5000);
                    setErrorAndSetOnUi(String.format("WebSocket private stream failure reason: %s", reason));
                } catch (Exception e) {
                }
                setStreamPrivate();
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                try {
                    Thread.sleep(5000);
                    setErrorAndSetOnUi(String.format("WebSocket private stream failure reason: %s", t.getMessage()));
                } catch (Exception e) {
                }
                setStreamPrivate();
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
        String signature = HmacSHA256Signer.auth(val, "r0IVwdjDXNR7gAlkuOycsj0VSsqMpIdhXHin");

        var args = List.of("GzLMsbb4tcLt0kCqv7", expires, signature);
        var authMap = Map.of("req_id", generateTransferID(), "op", "auth", "args", args);
        return JSON.toJSONString(authMap);
    }

    public static Map<String, List<String>> getOrdersMap() {
        return ORDERS_MAP;
    }

    public Map<String, Object> getOpenOrders() {
        TradeOrderRequest tradeOrderRequest = TradeOrderRequest.builder()
                .category(CategoryType.LINEAR)
                .symbol("BTCUSDT")
                .build();
        return (Map<String, Object>) orderRestClient.getOpenOrders(tradeOrderRequest);
    }

    @NotNull
    private Thread createPingThread(WebSocket ws) {
        return new Thread(() -> {
            while (true) {
                try {
                    if (ws != null) {
                        ws.send("{\"op\":\"ping\"}");
                        TimeUnit.SECONDS.sleep(5000);
                        continue;
                    }
                } catch (InterruptedException var3) {
                    setErrorAndSetOnUi(String.format("WebSocket failure pingpong: %s", var3.getMessage()));
                    setStreamPublic();
                }
                return;
            }
        });
    }

//    private void handleError(LinkedHashMap<String, Object> response, String errorMsg){
//        if (response == null) {
//            throw new OrderNotExecutedException(String.format(errorMsg, "ответ null."));
//        }
//        var retCode = response.get("retCode");
//        if (!Objects.equal(retCode, 0)) {
//            var message = response.get("retMsg");
//            throw new OrderNotExecutedException(String.format("Ошибка отмены ордеров. Message: %s.", message));
//        }
//    }

    public BigDecimal getPosition(String symbol) {
        var positionListRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).symbol(symbol).build();
        LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) positionRestClient.getPositionInfo(positionListRequest);
        var result = (LinkedHashMap<String, Object>) response.get("result");
        var data = (LinkedHashMap<String, Object>) ((List) result.get("list")).get(0);
        var side = (String) data.get("side");
        var size = BigDecimal.valueOf(Double.parseDouble((String) data.get("size")));
        if (side.toLowerCase().equals("buy")) {
            return size;
        }
        return size.multiply(BigDecimal.valueOf(-1.0d));
    }

    public void cancelOrders(String ticker) {
        TradeOrderRequest tradeOrderRequest = TradeOrderRequest.builder()
                .category(CategoryType.LINEAR)
                .baseCoin(ticker.replace("USDT", ""))
                .build();
        LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) orderRestClient.cancelAllOrder(tradeOrderRequest);
        var retCode = response.get("retCode");
        if (!Objects.equal(retCode, 0)) {
            var message = response.get("retMsg");
            throw new OrderNotExecutedException(String.format("Ошибка отмены ордеров. Message: %s.", message));
        }
    }

    private void gridOrders(OrderDirection direction, String ticker, StrategyOptions options, BigDecimal price) {
        for (int i = 0; i < options.getCountOfGrid(); i++) {
            price = direction == OrderDirection.ORDER_DIRECTION_BUY ? price.multiply(BigDecimal.valueOf(100L).add(options.getOffsetOfGrid()).divide(BigDecimal.valueOf(100L))) : price.multiply(BigDecimal.valueOf(100L).subtract(options.getOffsetOfGrid()).divide(BigDecimal.valueOf(100L)));
            var idOrder = UUID.randomUUID().toString();
//                modifyOrdersMap(idOrder, name);
            var response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .symbol(ticker)
                    .orderType(TradeOrderType.MARKET)
                    .orderLinkId(idOrder)
                    .triggerPrice(price.setScale(1, RoundingMode.HALF_UP).toString())
                    .tpTriggerBy(TriggerBy.LAST_PRICE)
                    .qty(options.getLotOfOneGrid().toString())
                    .triggerDirection(direction == OrderDirection.ORDER_DIRECTION_BUY ? 1 : 2)
                    .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                    .build());

            var retCode2 = response.get("retCode");
            if (!Objects.equal(retCode2, 0)) {
                var message = response.get("retMsg");
                throw new OrderNotExecutedException(String.format("Ошибка выставления сетки ордеров. Message: %s.", message));
            }
        }
    }
}
