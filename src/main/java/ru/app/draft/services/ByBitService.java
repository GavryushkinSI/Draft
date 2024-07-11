package ru.app.draft.services;

import com.alibaba.fastjson.JSON;
import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.TradeOrderType;
import com.bybit.api.client.domain.TriggerBy;
import com.bybit.api.client.domain.account.AccountType;
import com.bybit.api.client.domain.account.request.AccountDataRequest;
import com.bybit.api.client.domain.institution.LendingDataRequest;
import com.bybit.api.client.domain.market.InstrumentStatus;
import com.bybit.api.client.domain.position.request.PositionDataRequest;
import com.bybit.api.client.domain.trade.Side;
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.restApi.BybitApiAccountRestClient;
import com.bybit.api.client.restApi.BybitApiAsyncMarketDataRestClient;
import com.bybit.api.client.restApi.BybitApiLendingRestClient;
import com.bybit.api.client.restApi.BybitApiMarketRestClient;
import com.bybit.api.client.restApi.BybitApiPositionRestClient;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.bybit.api.client.security.HmacSHA256Signer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.CollectionUtils;
import ru.app.draft.exception.OrderNotExecutedException;
import org.springframework.stereotype.Service;
import ru.app.draft.models.ErrorData;
import ru.app.draft.models.Strategy;
import ru.app.draft.models.StrategyOptions;
import ru.app.draft.models.Ticker;
import ru.app.draft.models.UserCache;
import ru.app.draft.utils.CommonUtils;
import ru.app.draft.utils.DateUtils;
import ru.tinkoff.piapi.contract.v1.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.bybit.api.client.constant.Util.generateTransferID;
import static ru.app.draft.models.EventLog.*;
import static ru.app.draft.store.Store.LAST_PRICE;
import static ru.app.draft.store.Store.ORDERS_MAP;
import static ru.app.draft.store.Store.TICKERS_BYBIT;
import static ru.app.draft.store.Store.USER_STORE;
import static ru.app.draft.store.Store.modifyOrdersMap;
import static ru.app.draft.store.Store.updateLastPrice;

@SuppressWarnings("ALL")
@Service
@Slf4j
public class ByBitService extends AbstractTradeService {

    private static WebSocket webSocket;

    private final TelegramBotService telegramBotService;
    private final MarketDataStreamService streamService;
    private final BybitApiTradeRestClient orderRestClient;
    private final BybitApiPositionRestClient positionRestClient;
    private final BybitApiMarketRestClient marketRestClient;
    private final BybitApiLendingRestClient lendingRestClient;

    private final BybitApiAsyncMarketDataRestClient dataRestClient;

    private final BybitApiAccountRestClient accountClient;

    private final ObjectMapper mapper;

    private static final String PING_DATA = "{\"op\":\"ping\"}";

    public ByBitService(TelegramBotService telegramBotService, MarketDataStreamService streamService, BybitApiTradeRestClient orderRestClient, BybitApiPositionRestClient positionRestClient, BybitApiMarketRestClient marketRestClient, BybitApiLendingRestClient lendingRestClient, BybitApiAsyncMarketDataRestClient dataRestClient, BybitApiAccountRestClient accountClient, ObjectMapper mapper) {
        super(telegramBotService, streamService);
        this.telegramBotService = telegramBotService;
        this.streamService = streamService;
        this.orderRestClient = orderRestClient;
        this.positionRestClient = positionRestClient;
        this.marketRestClient = marketRestClient;
        this.lendingRestClient = lendingRestClient;
        this.dataRestClient = dataRestClient;
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
            if (strategy.getPositionTv() != null) {
                correctCurrentPosition(strategy);
                return;
            }
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
                Map<String, Object> result = sendOrder(direction, CommonUtils.formatBigDecimalNumber(position), changingStrategy.getTicker(), ordeId, triggerPrice.isPresent() ? String.valueOf(triggerPrice.get()) : null, isAmendOrder, changingStrategy.getOptions(), LAST_PRICE.get(changingStrategy.getFigi()).getPrice(), changingStrategy.getName(), strategy.getComment());
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

    public void correctCurrentPosition(@Nullable Strategy strategy) {

        getPosition(strategy.getTicker(), false);

        UserCache userCache = USER_STORE.get("Admin");
        List<Strategy> strategyList = userCache.getStrategies();

        Strategy changingStrategy = strategyList
                .stream()
                .filter(item -> item.getName().equals(strategy.getName())).findFirst().get();

        if (strategy.getQuantity().compareTo(BigDecimal.ZERO) != 0 && Math.abs(strategy.getQuantity().doubleValue()) < changingStrategy.getMinLot().doubleValue()) {
            log.info(String.format("[%s]=> quantity:%s", QUANTITY_LESS_MIN_LOT, strategy.getQuantity()));
            if (changingStrategy.getCurrentPosition().doubleValue() != 0) {
                log.info(String.format("[%s]=> ticker:%s, newCurrentPosition:0", EXIT_ORDERS, changingStrategy.getTicker()));
                closeOpenOrders(changingStrategy, userCache);
            }
            return;
        }

        if (strategy.getPositionTv().compareTo(changingStrategy.getCurrentPosition()) != 0) {
            var correctPosition = strategy.getPositionTv().subtract(changingStrategy.getCurrentPosition());
            OrderDirection direct = null;
            if (correctPosition.doubleValue() < 0) {
                direct = OrderDirection.ORDER_DIRECTION_SELL;
            }
            if (correctPosition.doubleValue() > 0) {
                direct = OrderDirection.ORDER_DIRECTION_BUY;
            }
            if (correctPosition.doubleValue() == 0) {
                direct = strategy.getCurrentPosition().doubleValue() > 0 ? OrderDirection.ORDER_DIRECTION_SELL : OrderDirection.ORDER_DIRECTION_BUY;
            }
            log.info(String.format("[%s]=> name:%s, currentPosition:%s, tvPosition:%s", NOT_MATCH_POSITION_WITH_TV, changingStrategy.getName(), changingStrategy.getCurrentPosition(), strategy.getPositionTv()));
            sendOrder(direct, CommonUtils.formatBigDecimalNumber(correctPosition), changingStrategy.getTicker(), UUID.randomUUID().toString(), null, false, changingStrategy.getOptions(), LAST_PRICE.get(changingStrategy.getFigi()).getPrice(), changingStrategy.getName(), null);
        }
    }

    public synchronized Map<String, Object> setCurrentPosition(@Nullable Strategy strategy, String orderLinkedId, BigDecimal executionPrice, BigDecimal lastPrice, String side, BigDecimal execQty, BigDecimal currentPosition, String symbol) {
        UserCache userCache = USER_STORE.get("Admin");
        List<Strategy> strategyList = userCache.getStrategies();

        if (symbol != null && !CollectionUtils.isEmpty(strategyList)) {
            Optional<Strategy> execStrategy = strategyList
                    .stream()
                    .filter(item -> item.getFigi().equals(symbol)).findFirst();
            if (execStrategy.isPresent()) {
                var exec = execStrategy.get();
                if (exec.getCurrentPosition().compareTo(currentPosition) != 0) {
                    log.info(String.format("[%s]=> currentPosition:%s, newCurrentPosition:%s", CORRECT_CURRENT_POS, exec.getCurrentPosition(), currentPosition));
                    exec.setCurrentPosition(currentPosition);
                    userCache.setStrategies(strategyList);
                    USER_STORE.replace("Admin", userCache);
                }
            }
            return null;
        }

        if (orderLinkedId != null) {
            Optional<Map.Entry<String, List<String>>> result = ORDERS_MAP.entrySet()
                    .stream()
                    .filter((it) -> {
                        return it.getValue().contains(orderLinkedId);
                    })
                    .findFirst();

            if (result.isPresent()) {
                Strategy execStrategy = strategyList
                        .stream()
                        .filter(item -> item.getName().equals(result.get().getKey()))
                        .findFirst()
                        .get();

                if (execStrategy != null) {
                    execStrategy.setDirection(side.toLowerCase());
                    if (side.toLowerCase().equals("sell")) {
                        execQty = execQty.multiply(BigDecimal.valueOf(-1.0d));
                    }
                    log.info(String.format("[%s]=> name:%s, currentPosition:%s, execQty:%s, executionPrice:%s, side:%s", SET_CURRENT_POS_AFTER_EXECUTE, execStrategy.getName(), execStrategy.getCurrentPosition(), execQty, executionPrice, side));
                    updateStrategyCache(strategyList, execStrategy, execStrategy, executionPrice, userCache, execQty, DateUtils.getCurrentTime(), false);
                    getPosition(execStrategy.getTicker(), false);
                }
            }

            return null;
        }

        if (strategy == null) {
            return null;
        }

        Strategy changingStrategy = strategyList
                .stream()
                .filter(item -> item.getName().equals(strategy.getName())).findFirst().get();

        OrderDirection direction = null;

        if (strategy.getQuantity().compareTo(BigDecimal.ZERO) != 0 && Math.abs(strategy.getQuantity().doubleValue()) < changingStrategy.getMinLot().doubleValue()) {
            log.info(String.format("[%s]=> quantity:%s", QUANTITY_LESS_MIN_LOT, strategy.getQuantity()));
            return null;
        }

        var comment = strategy.getComment();
        //Закрываем текущую позицию в ноль
        if (comment != null && comment.contains("exit")) {
            if (changingStrategy.getCurrentPosition().doubleValue() != 0) {
                log.info(String.format("[%s]=> ticker:%s, newCurrentPosition:0", EXIT_ORDERS, changingStrategy.getTicker()));
                closeOpenOrders(changingStrategy, userCache);
            }
            return null;
        }

        //Закрываем ордеры которые не исполнены
        if (comment != null && comment.contains("cancel")) {
            cancelOrders(changingStrategy.getTicker(), false);
            log.info(String.format("[%s]=> ticker:%s, close_condition_orders", CANCEL_CONDITIONAL_ORDERS, changingStrategy.getTicker()));
            return null;
        }

        //Не открывать ордер если он уже есть такого же объёма
        if (strategy.getQuantity().compareTo(changingStrategy.getCurrentPosition()) == 0) {
            if (strategy.getQuantity().doubleValue() == 0) {
                cancelOrders(changingStrategy.getTicker(), false);
                log.info(String.format("[%s]=> quantity:%s", CANCEL_CONDITIONAL_ORDERS, strategy.getQuantity()));
            }
            return null;
        }

        //REVERSE
        if ((strategy.getDirection().equals("buy") && changingStrategy.getCurrentPosition().doubleValue() > 0
                || strategy.getDirection().equals("sell") && changingStrategy.getCurrentPosition().doubleValue() < 0) && Boolean.TRUE.equals(changingStrategy.getDoReverse())) {
            if (changingStrategy.getCurrentPosition().doubleValue() != 0) {
                closeOpenOrders(changingStrategy, userCache);
                log.info(String.format("[%s]=> quantity:%s", CLOSE_OPEN_ORDERS_OR_REVERSE, strategy.getQuantity()));
            }
            return null;
        }

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
        }

        BigDecimal position = strategy.getQuantity().subtract(changingStrategy.getCurrentPosition());

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

    void getAccountInfo() {
        var walletBalanceRequest = AccountDataRequest.builder().accountType(AccountType.CONTRACT).coin("USDT").build();
        var data = ((Map<String, Object>) (accountClient.getWalletBalance(walletBalanceRequest)));
    }

    LinkedHashMap<String, Object> sendOrder(OrderDirection direction, String position, String ticker, String orderId, String triggerPrice, Boolean isAmendOrder, StrategyOptions options, BigDecimal executionPrice, String name, String comment) {
        TradeOrderRequest tradeOrderRequest = null;
        LinkedHashMap<String, Object> response = null;
        List<String> list = ORDERS_MAP.get(name);
        if (CollectionUtils.isEmpty(list)) {
            list = new ArrayList<>();
            ORDERS_MAP.put(name, list);
        } else {
            list.clear();
        }
        modifyOrdersMap(orderId, name);
        if (triggerPrice == null && options.getUseGrid() && comment != null && comment.contains("grid")) {
            tradeOrderRequest = TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .symbol(ticker)
                    .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                    .orderType(TradeOrderType.MARKET)
                    .orderLinkId(orderId)
                    .qty(position)
                    .build();
            response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
            var retCode3 = response.get("retCode");
            if (!Objects.equal(retCode3, 0)) {
                var message = response.get("retMsg");
                throw new OrderNotExecutedException(String.format("Ошибка исполнения рыночного ордера. Message: %s.", message));
            }

            gridOrders(direction, ticker, options, executionPrice, name);
        }
        if (comment == null || !comment.contains("grid")) {
            if (triggerPrice == null) {
                tradeOrderRequest = TradeOrderRequest.builder()
                        .category(CategoryType.LINEAR)
                        .symbol(ticker)
                        .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                        .orderType(TradeOrderType.MARKET)
                        .orderLinkId(orderId)
                        .qty(position)
                        .build();
                response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
                log.info(String.format("[%s]=> qty:%s", MARKET_ORDER_EXECUTE, position));
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
                            .qty(position)
                            .build();
                    response = (LinkedHashMap<String, Object>) orderRestClient.amendOrder(tradeOrderRequest);
                } else {
                    //trigerPrice!=null
                    cancelOrders(ticker, false);
                    tradeOrderRequest = TradeOrderRequest.builder()
                            .category(CategoryType.LINEAR)
                            .symbol(ticker)
                            .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                            .orderType(TradeOrderType.MARKET)
                            .orderLinkId(orderId)
                            .triggerPrice(triggerPrice)
                            .triggerDirection(direction == OrderDirection.ORDER_DIRECTION_BUY ? 1 : 2)
                            .tpTriggerBy(TriggerBy.LAST_PRICE)
                            .qty(position)
                            .build();
                    response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);

                    if (options.getUseGrid() && comment != null && comment.contains("grid")) {
                        gridOrders(direction, ticker, options, executionPrice, name);
                    }
                }
            }
        }
        var retCode = response.get("retCode");

        if (Objects.equal(retCode, 110092) || Objects.equal(retCode, 110093)) {
            log.info(String.format("[%s]=> ticker:%s, triggerPrice:%s", ORDER_CANNOT_EXECUTE, ticker, triggerPrice));
            tradeOrderRequest = TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .symbol(ticker)
                    .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                    .orderType(TradeOrderType.MARKET)
                    .orderLinkId(orderId)
                    .qty(position)
                    .build();
            response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
            log.info(String.format("[%s]: symbol:%s, qty:%s", MARKET_ORDER_EXECUTE_FORCE, ticker, position));
        }
        var retCode2 = response.get("retCode");
        if (!Objects.equal(retCode2, 0) && !Objects.equal(retCode2, 110092) && !Objects.equal(retCode2, 110093)) {
            var message = response.get("retMsg");
            throw new OrderNotExecutedException(String.format("Ошибка исполнения ордера %s, %s, %s. Message: %s.", ticker, direction.name(), position, message));
        }
        return response;
    }

    public Ticker getFigi(List<String> tickers) {
        return TICKERS_BYBIT.get("tickers").stream().filter(i -> i.getValue().equals(tickers.get(0))).findFirst().get();
    }

    private static List<String> getAllTickers() {
        UserCache userCache = USER_STORE.get("Admin");
        List<Strategy> strategyList = userCache.getStrategies();
        if (CollectionUtils.isEmpty(strategyList)) {
            return List.of("tickers.BTCUSDT");
        }
        return strategyList.stream().map(i -> String.format("tickers.%s", i.getTicker())).collect(Collectors.toList());
    }

    public void setStreamPublic() {
        Request request = new Request.Builder().url("wss://stream.bybit.com/v5/public/linear").build();
        OkHttpClient publicClient = new OkHttpClient.Builder().build();
        Map<String, Object> subscribeMsg = new LinkedHashMap<>();
        subscribeMsg.put("op", "subscribe");
        subscribeMsg.put("req_id", generateTransferID());
        subscribeMsg.put("args", getAllTickers());

        webSocket = publicClient.newWebSocket(request, new WebSocketListener() {

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

    public void sendInPublicWebSocket(String ticker) {
        if (webSocket != null) {
            Map<String, Object> subscribeMsg = new LinkedHashMap<>();
            subscribeMsg.put("op", "subscribe");
            subscribeMsg.put("req_id", generateTransferID());
            subscribeMsg.put("args", List.of(String.format("tickers.%s", ticker)));
            webSocket.send(JSON.toJSONString(subscribeMsg));
        }
    }

    public void setStreamPrivate() {
        OkHttpClient privateClient = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url("wss://stream.bybit.com/v5/private?max_alive_time=8m").build();
        Map<String, Object> subscribeMsg = new LinkedHashMap<>();
        subscribeMsg.clear();
        subscribeMsg.put("op", "subscribe");
        subscribeMsg.put("req_id", generateTransferID());
        subscribeMsg.put("args", List.of("execution.linear"
                /*   "order"*/
        ));
        WebSocket privateWebSocket = privateClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                Thread pingThread = createPingThread2(webSocket);
                pingThread.setName("thread-private-ping");
                pingThread.start();
                webSocket.send(createAuthMessage());
                webSocket.send(JSON.toJSONString(subscribeMsg));
                log.info("success_bybit_private_stream");
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, String text) {
                if (text != null) {
                    try {
                        Map<String, Object> result = (Map<String, Object>) JSON.parse(text);
                        String topic = (String) result.get("topic");
                        if (topic != null && topic.equals("execution.linear")) {
                            var data = ((List<Object>) result.get("data"));
                            if (data != null) {
                                log.info(String.format("[%s]=> data size:%s", STREAM_EXECUTE_POSITION, data.size()));
                                data.forEach(it -> {
                                    Map<String, Object> map = ((Map<String, Object>) it);
                                    BigDecimal execPrice = BigDecimal.valueOf(Double.parseDouble((String) map.get("execPrice")));
                                    String side = (String) map.get("side");
                                    BigDecimal execQty = BigDecimal.valueOf(Double.parseDouble((String) map.get("execQty")));
                                    String orderLinkId = (String) map.get("orderLinkId");
                                    setCurrentPosition(null, orderLinkId, execPrice, null, side, execQty, null, null);
                                });
                            }
                        }
                    } catch (Exception ex) {
                        setErrorAndSetOnUi(String.format("WebSocket failure reason onMesagePrivate: %s. Text:%s", ex.getMessage(), text));
                    }
                }
            }

            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, String reason) {
                try {
                    setErrorAndSetOnUi(String.format("WebSocket private stream closed reason: %s", reason));
                    Thread.sleep(300);
                    setStreamPrivate();
                } catch (Exception e) {
                    setErrorAndSetOnUi(String.format("WebSocket private stream closed reason two: %s", reason));
                }
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                try {
                    setErrorAndSetOnUi(String.format("WebSocket private stream failure reason: %s", t.getMessage()));
                    Thread.sleep(300);
                    setStreamPrivate();
                } catch (Exception e) {
                    setErrorAndSetOnUi(String.format("WebSocket private stream failure reason two: %s", t.getMessage()));
                }
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
        String signature = HmacSHA256Signer.auth(val, "cEEy2UikJip79bTn2RcPy7Do0dgiZ5LxRPl7");

        var args = List.of("g6pMUO4vmwTHZGsotf", expires, signature);
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

    @NotNull
    private Thread createPingThread2(WebSocket ws) {
        return new Thread(() -> {
            while (true) {
                try {
                    if (ws != null) {
                        ws.send("{\"op\":\"ping\"}");
                        TimeUnit.SECONDS.sleep(18);
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

    public void getPosition(String symbol, boolean isNew) {
        var positionListRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).symbol(symbol).settleCoin("USDT").build();
        LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) positionRestClient.getPositionInfo(positionListRequest);
        var result = (LinkedHashMap<String, Object>) response.get("result");
        var data = (List) result.get("list");
        if (data != null && data.size() > 0) {
            data.forEach(new Consumer() {
                @Override
                public void accept(Object o) {
                    var it = (LinkedHashMap<String, Object>) o;
                    var side = (String) it.get("side");
                    var symbol = (String) it.get("symbol");
                    var size = BigDecimal.valueOf(Double.parseDouble((String) it.get("size")))
                            .multiply(side.toLowerCase().equals("buy") ? BigDecimal.ONE : BigDecimal.valueOf(-1.0d));
                    UserCache userCache = USER_STORE.get("Admin");
                    List<Strategy> strategyList = userCache.getStrategies();
                    if (!CollectionUtils.isEmpty(strategyList)) {
                        Optional<Strategy> execStrategy = strategyList
                                .stream()
                                .filter(item -> item.getFigi().equals(symbol)).findFirst();
                        if (execStrategy.isPresent()) {
                            var exec = execStrategy.get();
                            if (exec.getCurrentPosition().compareTo(size) != 0) {
                                exec.setCurrentPosition(size);
                                userCache.setStrategies(strategyList);
                                USER_STORE.replace("Admin", userCache);
                                if (!isNew) {
                                    log.info(String.format("[%s]=> currentPosition:%s, newCurrentPosition:%s", CORRECT_CURRENT_POS, exec.getCurrentPosition(), size));
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    public void cancelOrders(String ticker, boolean all) {
        TradeOrderRequest tradeOrderRequest = null;

        if (all) {
            tradeOrderRequest = TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .build();
        } else {
            tradeOrderRequest = TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .symbol(ticker)
                    .build();
        }
        LinkedHashMap<String, Object> response = (LinkedHashMap<String, Object>) orderRestClient.cancelAllOrder(tradeOrderRequest);
        var retCode = response.get("retCode");
        if (!Objects.equal(retCode, 0)) {
            var message = response.get("retMsg");
            throw new OrderNotExecutedException(String.format("Ошибка отмены ордеров. Message: %s.", message));
        }
    }

    private void gridOrders(OrderDirection direction, String ticker, StrategyOptions options, BigDecimal price, String name) {
        for (int i = 0; i < options.getCountOfGrid(); i++) {
            price = direction == OrderDirection.ORDER_DIRECTION_BUY ? price.multiply(BigDecimal.valueOf(100L).add(options.getOffsetOfGrid()).divide(BigDecimal.valueOf(100L))) : price.multiply(BigDecimal.valueOf(100L).subtract(options.getOffsetOfGrid()).divide(BigDecimal.valueOf(100L)));
            var idOrder = UUID.randomUUID().toString();
            modifyOrdersMap(idOrder, name);
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

    private void closeOpenOrders(Strategy changingStrategy, UserCache userCache) {
        List<String> list = ORDERS_MAP.get(changingStrategy.getName());
        if (CollectionUtils.isEmpty(list)) {
            list = new ArrayList<>();
            ORDERS_MAP.put(changingStrategy.getName(), list);
        }

        Side direction = null;
        if (changingStrategy.getCurrentPosition().doubleValue() > 0) {
            direction = Side.SELL;
        } else {
            direction = Side.BUY;
        }

        var tradeOrderRequest = TradeOrderRequest.builder()
                .category(CategoryType.LINEAR)
                .symbol(changingStrategy.getTicker())
                .side(direction)
                .orderType(TradeOrderType.MARKET)
                .orderLinkId(UUID.randomUUID().toString())
                .qty(String.valueOf(Math.abs(changingStrategy.getCurrentPosition().doubleValue())))
                .build();

        var response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
        var strategyList = userCache.getStrategies();
        var strategy = strategyList.stream().filter(i -> i.getId().equals(changingStrategy.getId())).findFirst().get();
        if (!Objects.equal(response.get("retCode"), 0)) {
            var message = response.get("retMsg");
            var errorData = new ErrorData();
            errorData.setMessage(String.format("Ошибка закрытия открытого ордера. Message: %s.", message));
            errorData.setTime(DateUtils.getCurrentTime());
            strategyList.stream().forEach(i -> {
                if (i.getId().equals(changingStrategy.getId())) {
                    i.setErrorData(errorData);
                }
            });
            userCache.setStrategies(strategyList);
            USER_STORE.replace("Admin", userCache);
            sendMessageInSocket(userCache.getStrategies());
            return;
        }

        strategyList.stream().forEach(i -> {
            if (i.getId().equals(changingStrategy.getId())) {
                i.setCurrentPosition(BigDecimal.ZERO);
            }
        });
        userCache.setStrategies(strategyList);
        USER_STORE.replace("Admin", userCache);
        sendMessageInSocket(userCache.getStrategies());
    }

    public void getInstrumentsInfo() {
        List<Ticker> byBitTickers = new ArrayList<>();
        List<Ticker> byBitTickers2 = new ArrayList<>();
        dataRestClient.getInstrumentsInfo(com.bybit.api.client.domain.market.request.MarketDataRequest.builder().category(CategoryType.LINEAR).instrumentStatus(InstrumentStatus.TRADING).limit(1000).build(), (data) -> {
            var response = (LinkedHashMap<String, Object>) data;
            var result = (LinkedHashMap<String, Object>) response.get("result");
            var data2 = (List) result.get("list");
            data2.forEach(new Consumer() {
                @Override
                public void accept(Object o) {
                    var it = (LinkedHashMap<String, Object>) o;
                    var symbol = (String) it.get("symbol");
                    var settleCoin = (String) it.get("settleCoin");
                    var priceScale = (String) it.get("priceScale");
                    var lotSizeFilter = (LinkedHashMap<String, Object>) it.get("lotSizeFilter");
                    var minOrderQty = BigDecimal.valueOf(Double.valueOf((String) lotSizeFilter.get("minOrderQty")));
                    var leverageFilter = (LinkedHashMap<String, Object>) it.get("leverageFilter");
                    if (settleCoin.equals("USDT") && Double.valueOf((String) leverageFilter.get("maxLeverage")) >= 25d) {
                        if (Integer.valueOf(priceScale) <= 3) {
                            byBitTickers.add(
                                    new Ticker(symbol, symbol, symbol, "BYBITFUT", minOrderQty, priceScale)
                            );
                        } else {
                            byBitTickers2.add(
                                    new Ticker(symbol, symbol, symbol, "BYBITFUT", minOrderQty, priceScale)
                            );
                        }
                    }
                }
            });
            byBitTickers.addAll(byBitTickers2);
            TICKERS_BYBIT.replace("tickers", byBitTickers);
        });
    }
}