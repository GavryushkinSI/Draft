package ru.app.draft.services;

import com.alibaba.fastjson.JSON;
import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.TradeOrderType;
import com.bybit.api.client.domain.TriggerBy;
import com.bybit.api.client.domain.account.AccountType;
import com.bybit.api.client.domain.account.request.AccountDataRequest;
import com.bybit.api.client.domain.institution.LendingDataRequest;
import com.bybit.api.client.domain.market.InstrumentStatus;
import com.bybit.api.client.domain.market.MarketInterval;
import com.bybit.api.client.domain.market.request.MarketDataRequest;
import com.bybit.api.client.domain.position.request.PositionDataRequest;
import com.bybit.api.client.domain.trade.PositionIdx;
import com.bybit.api.client.domain.trade.Side;
import com.bybit.api.client.domain.trade.StopOrderType;
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.restApi.*;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import ru.app.draft.entity.Candle;
import ru.app.draft.exception.OrderNotExecutedException;
import org.springframework.stereotype.Service;
import ru.app.draft.models.ConditionalOrder;
import ru.app.draft.models.ErrorData;
import ru.app.draft.models.Pnl;
import ru.app.draft.models.StrategyOptions;
import ru.app.draft.models.StrategyTv;
import ru.app.draft.models.TelegramSignal;
import ru.app.draft.models.TestTelegramChannel;
import ru.app.draft.models.Ticker;
import ru.app.draft.models.UserCache;
import ru.app.draft.utils.CommonUtils;
import ru.app.draft.utils.DateUtils;
import ru.tinkoff.piapi.contract.v1.OrderDirection;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.bybit.api.client.constant.Util.generateTransferID;
import static ru.app.draft.models.EventLog.CANCEL_CONDITIONAL_ORDERS;
import static ru.app.draft.models.EventLog.CLOSE_OPEN_ORDERS_OR_REVERSE;
import static ru.app.draft.models.EventLog.CORRECT_CURRENT_POS;
import static ru.app.draft.models.EventLog.ERROR;
import static ru.app.draft.models.EventLog.EXIT_ORDERS;
import static ru.app.draft.models.EventLog.MARKET_ORDER_EXECUTE;
import static ru.app.draft.models.EventLog.MARKET_ORDER_EXECUTE_FORCE;
import static ru.app.draft.models.EventLog.NOT_MATCH_POSITION_WITH_TV;
import static ru.app.draft.models.EventLog.ORDER_CANNOT_EXECUTE;
import static ru.app.draft.models.EventLog.QUANTITY_LESS_MIN_LOT;
import static ru.app.draft.models.EventLog.SET_CURRENT_POS_AFTER_EXECUTE;
import static ru.app.draft.models.EventLog.STREAM_EXECUTE_POSITION;
import static ru.app.draft.store.Store.*;
import static ru.app.draft.utils.CommonUtils.*;

@SuppressWarnings("ALL")
@Service
@Slf4j
public class ByBitService extends AbstractTradeService {

    @Value("${bybit.key}")
    private String key;
    @Value("${bybit.serial}")
    private String serial;
    private static final String FILE_NAME = "candles.dat";

    private static List<String> telegramSignalList = new ArrayList();
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

    public ByBitService(TelegramBotService telegramBotService, MarketDataStreamService streamService, BybitApiTradeRestClient orderRestClient, BybitApiPositionRestClient positionRestClient, BybitApiMarketRestClient marketRestClient, BybitApiLendingRestClient lendingRestClient, BybitApiMarketRestClient syncDataRestClient, BybitApiAsyncMarketDataRestClient dataRestClient, BybitApiAccountRestClient accountClient, ObjectMapper mapper) {
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

    public void getAllTickers(List<String> filter) {
        this.getInstrumentsInfo();
    }

    private void telegramSignal(StrategyTv strategyTv) throws InterruptedException {
        var telegramSignal = strategyTv.getTelegramSignal();

        if (telegramSignalList.contains(strategyTv.getTicker())) {
            var res = CommonUtils.parseResponse(getPositionInfo(strategyTv.getTicker()), "size");
            if (res == null || Double.parseDouble(String.valueOf(res)) == 0) {
                cancelOrders(strategyTv.getTicker(), false, null);
                telegramSignalList.remove(strategyTv.getTicker());
                telegramSignalList.add(strategyTv.getTicker());
            } else {
                return;
            }
        } else {
            telegramSignalList.add(strategyTv.getTicker());
        }

        var ticker = getFigi(List.of(strategyTv.getTicker()));
        var minLot = ticker.getMinLot().doubleValue();
        var quantity = BigDecimal.valueOf(Math.ceil(50.0d / Double.parseDouble(telegramSignal.getStop()) / minLot)).multiply(ticker.getMinLot());

        TradeOrderRequest tradeOrderRequest = null;
        tradeOrderRequest = TradeOrderRequest.builder()
                .category(CategoryType.LINEAR)
                .symbol(strategyTv.getTicker())
                .side(strategyTv.getDirection().equals("buy") ? Side.BUY : Side.SELL)
                .orderType(TradeOrderType.MARKET)
                .orderLinkId(UUID.randomUUID().toString())
                .takeProfit(telegramSignal.getTake2())
                .stopLoss(telegramSignal.getStop())
                .stopOrderType(StopOrderType.STOP_LOSS)
                .qty(String.valueOf(quantity))
                .build();
        try {
            var response1 = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
            var entry1 = telegramSignal.getEntry1() != null ? Double.parseDouble(telegramSignal.getEntry1()) : null;
            var entry2 = telegramSignal.getEntry2() != null ? Double.parseDouble(telegramSignal.getEntry2()) : Double.parseDouble(telegramSignal.getStop());
            if (entry1 != null) {
                var delemiter = Math.abs(BigDecimal.valueOf(entry1).subtract(BigDecimal.valueOf(entry2)).divide(BigDecimal.valueOf(3L), RoundingMode.HALF_EVEN).doubleValue());
                int i = 3;
                while (i != 0) {
                    var triggerPrice = formatNumber(BigDecimal.valueOf(strategyTv.getDirection().equals("buy") ? entry1 - i * delemiter : i + delemiter), countDecimalPlaces(telegramSignal.getEntry1()));
                    tradeOrderRequest = TradeOrderRequest.builder()
                            .category(CategoryType.LINEAR)
                            .price(String.valueOf(triggerPrice))
                            .symbol(strategyTv.getTicker())
                            .side(strategyTv.getDirection().equals("buy") ? Side.BUY : Side.SELL)
                            .orderType(TradeOrderType.LIMIT)
                            .orderLinkId(UUID.randomUUID().toString())
                            .triggerDirection(strategyTv.getDirection().equals("buy") ? 1 : 2)
                            .qty(String.valueOf(quantity))
                            .build();
                    i--;
                    orderRestClient.createOrder(tradeOrderRequest);
                }
            }
        } catch (Exception exception) {
            throw exception;
        }
    }

    @Override
    public void sendSignal(StrategyTv strategyTv) throws InterruptedException {
        if (strategyTv.getName().equals("telegram")) {
            telegramSignal(strategyTv);
            return;
        }
        ErrorData errorData = null;
        StrategyTv changingStrategyTv = null;
        Map<String, Object> map = null;
        BigDecimal executionPrice = null;
        BigDecimal position = null;
        UserCache userCache = null;
        List<StrategyTv> strategyTvList = null;
        try {
            if (Boolean.FALSE.equals(strategyTv.getIsActive())) {
                return;
            }
            if (strategyTv.getPositionTv() != null) {
                Thread.sleep(3000);
                correctCurrentPosition(strategyTv);
                return;
            }
            map = setCurrentPosition(strategyTv, null, null, null, null, null, null, null, null);

            if (map != null) {
                userCache = USER_STORE.get(strategyTv.getUserName());
                strategyTvList = userCache.getStrategies();
            } else {
                return;
            }

            changingStrategyTv = (StrategyTv) map.get("changingStrategy");
            if (!changingStrategyTv.getIsActive()) {
                return;
            }

            position = (BigDecimal) map.get("position");
            OrderDirection direction = (OrderDirection) map.get("direction");
            if (changingStrategyTv.getConsumer().contains("terminal")) {
                var ordeId = (String) map.get("orderId");
                var triggerPrice = (Optional<BigDecimal>) map.get("triggerPrice");
                var isAmendOrder = (Boolean) map.get("isAmendOrder");
                Map<String, Object> result = sendOrder(direction, CommonUtils.formatBigDecimalNumber(position), changingStrategyTv.getTicker(), ordeId, triggerPrice.isPresent() ? String.valueOf(triggerPrice.get()) : null, isAmendOrder, changingStrategyTv.getOptions(), LAST_PRICE.get(changingStrategyTv.getFigi()).getPrice(), changingStrategyTv.getName(), strategyTv.getComment(), changingStrategyTv.getCurrentPosition(), strategyTv.getTpl());
                executionPrice = LAST_PRICE.get(changingStrategyTv.getFigi()).getPrice();
            } else if (changingStrategyTv.getConsumer().contains("test")) {
                executionPrice = LAST_PRICE.get(changingStrategyTv.getFigi()).getPrice();
            }
        } catch (Exception e) {
            if (changingStrategyTv == null) {
                userCache = USER_STORE.get(strategyTv.getUserName());
                strategyTvList = userCache.getStrategies();
                changingStrategyTv = strategyTvList
                        .stream()
                        .filter(str -> str.getName().equals(strategyTv.getName())).findFirst().get();
            }
            errorData = new ErrorData();
            errorData.setMessage("Ошибка: " + e.getMessage());
            errorData.setTime(DateUtils.getCurrentTime());
            changingStrategyTv.setErrorData(errorData);
        }
        if (errorData != null) {
            sendMessageInSocket(strategyTvList);
        }
        if (changingStrategyTv.getConsumer().contains("test")) {
            String time = DateUtils.getCurrentTime();
            updateStrategyCache(strategyTvList, strategyTv, changingStrategyTv, executionPrice, userCache, position, time, false, null);
        }
    }

    public void correctCurrentPosition(@Nullable StrategyTv strategyTv) {
        getPosition(strategyTv.getTicker(), false);

        UserCache userCache = USER_STORE.get("Admin");
        List<StrategyTv> strategyTvList = userCache.getStrategies();

        StrategyTv changingStrategyTv = strategyTvList
                .stream()
                .filter(item -> item.getName().equals(strategyTv.getName())).findFirst().get();

        if (changingStrategyTv.getOptions().getUseGrid()) {
            List<ConditionalOrder> list = ORDERS_MAP.get(strategyTv.getName());

            if (Math.signum(strategyTv.getPositionTv().doubleValue()) != Math.signum(changingStrategyTv.getCurrentPosition().doubleValue())) {
                cancelOrders(changingStrategyTv.getTicker(), false, null);
                simpleCorrectPosition(strategyTv, changingStrategyTv, null, strategyTv.getComment(), changingStrategyTv.getCurrentPosition());
            } else {
                //Пока не используем
//                if (Math.abs(strategyTv.getQuantity().doubleValue()) > Math.abs(changingStrategyTv.getCurrentPosition().doubleValue())) {
//                    simpleCorrectPosition(strategyTv, changingStrategyTv, null, null, null);
//                }
            }
        } else {
            if (strategyTv.getPositionTv().compareTo(BigDecimal.ZERO) != 0 && Math.abs(strategyTv.getPositionTv().doubleValue()) < changingStrategyTv.getMinLot().doubleValue()) {
                log.info(String.format("[%s]=> quantity:%s", QUANTITY_LESS_MIN_LOT, strategyTv.getPositionTv()));
                if (changingStrategyTv.getCurrentPosition().doubleValue() != 0) {
                    log.info(String.format("[%s]=> ticker:%s, newCurrentPosition:0", EXIT_ORDERS, changingStrategyTv.getTicker()));
                    closeOpenOrders(changingStrategyTv, userCache);
                }
                return;
            }

            if (strategyTv.getPositionTv().compareTo(changingStrategyTv.getCurrentPosition()) != 0) {
                simpleCorrectPosition(strategyTv, changingStrategyTv, null, null, null);
            }
        }
    }

    private void simpleCorrectPosition(StrategyTv strategyTv, StrategyTv changingStrategyTv, String triggerPrice, String comment, BigDecimal currentPosition) {
        var correctPosition = strategyTv.getPositionTv().subtract(changingStrategyTv.getCurrentPosition());
        OrderDirection direct = null;
        if (correctPosition.doubleValue() < 0) {
            direct = OrderDirection.ORDER_DIRECTION_SELL;
        }
        if (correctPosition.doubleValue() > 0) {
            direct = OrderDirection.ORDER_DIRECTION_BUY;
        }
        if (correctPosition.doubleValue() == 0) {
            direct = strategyTv.getCurrentPosition().doubleValue() > 0 ? OrderDirection.ORDER_DIRECTION_SELL : OrderDirection.ORDER_DIRECTION_BUY;
        }
        log.info(String.format("[%s]=> name:%s, currentPosition:%s, tvPosition:%s", NOT_MATCH_POSITION_WITH_TV, changingStrategyTv.getName(), changingStrategyTv.getCurrentPosition(), strategyTv.getPositionTv()));
        sendOrder(direct, CommonUtils.formatBigDecimalNumber(correctPosition), changingStrategyTv.getTicker(), UUID.randomUUID().toString(), triggerPrice, false, changingStrategyTv.getOptions(), LAST_PRICE.get(changingStrategyTv.getFigi()).getPrice(), changingStrategyTv.getName(), comment, currentPosition, null);
    }

    public synchronized Map<String, Object> setCurrentPosition(@Nullable StrategyTv strategyTv, String orderLinkedId, BigDecimal executionPrice, BigDecimal lastPrice, String side, BigDecimal execQty, BigDecimal currentPosition, String symbol, String orderId) {
        UserCache userCache = USER_STORE.get("Admin");
        List<StrategyTv> strategyTvList = userCache.getStrategies();

        if (symbol != null && !CollectionUtils.isEmpty(strategyTvList)) {
            Optional<StrategyTv> execStrategy = strategyTvList
                    .stream()
                    .filter(item -> item.getFigi().equals(symbol)).findFirst();
            if (execStrategy.isPresent()) {
                var exec = execStrategy.get();
                if (exec.getCurrentPosition().compareTo(currentPosition) != 0) {
                    log.info(String.format("[%s]=> currentPosition:%s, newCurrentPosition:%s", CORRECT_CURRENT_POS, exec.getCurrentPosition(), currentPosition));
                    exec.setCurrentPosition(currentPosition);
                    userCache.setStrategies(strategyTvList);
                    USER_STORE.replace("Admin", userCache);
                }
            }
            return null;
        }

        if (orderLinkedId != null) {
            Optional<Map.Entry<String, List<ConditionalOrder>>> result = ORDERS_MAP.entrySet()
                    .stream()
                    .filter((it) -> {
                        return it.getValue().stream().map(i -> i.getOrderLinkId()).collect(Collectors.toList()).contains(orderLinkedId);
                    })
                    .findFirst();

            if (result.isPresent()) {
                StrategyTv execStrategyTv = strategyTvList
                        .stream()
                        .filter(item -> item.getName().equals(result.get().getKey()))
                        .findFirst()
                        .get();

                if (execStrategyTv != null) {
                    execStrategyTv.setDirection(side.toLowerCase());
                    if (side.toLowerCase().equals("sell")) {
                        execQty = execQty.multiply(BigDecimal.valueOf(-1.0d));
                    }
                    log.info(String.format("[%s]=> name:%s, currentPosition:%s, execQty:%s, executionPrice:%s, side:%s", SET_CURRENT_POS_AFTER_EXECUTE, execStrategyTv.getName(), execStrategyTv.getCurrentPosition(), execQty, executionPrice, side));
                    updateStrategyCache(strategyTvList, execStrategyTv, execStrategyTv, executionPrice, userCache, execQty, DateUtils.getCurrentTime(), false, orderLinkedId);
                    getPosition(execStrategyTv.getTicker(), false);
                }
            }

            return null;
        }

        if (strategyTv == null) {
            return null;
        }

        StrategyTv changingStrategyTv = strategyTvList
                .stream()
                .filter(item -> item.getName().equals(strategyTv.getName())).findFirst().get();

        OrderDirection direction = null;

        if (strategyTv.getQuantity().compareTo(BigDecimal.ZERO) != 0 && Math.abs(strategyTv.getQuantity().doubleValue()) < changingStrategyTv.getMinLot().doubleValue()) {
            log.info(String.format("[%s]=> quantity:%s", QUANTITY_LESS_MIN_LOT, strategyTv.getQuantity()));
            return null;
        }

        var comment = strategyTv.getComment();
        //Закрываем текущую позицию в ноль
        if (comment != null && comment.contains("exit")) {
            if (changingStrategyTv.getCurrentPosition().doubleValue() != 0) {
                log.info(String.format("[%s]=> ticker:%s, newCurrentPosition:0", EXIT_ORDERS, changingStrategyTv.getTicker()));
                closeOpenOrders(changingStrategyTv, userCache);
            }
            return null;
        }

        //Закрываем ордеры которые не исполнены
        if (comment != null && comment.contains("cancel")) {
            cancelOrders(changingStrategyTv.getTicker(), false, null);
            log.info(String.format("[%s]=> ticker:%s, close_condition_orders", CANCEL_CONDITIONAL_ORDERS, changingStrategyTv.getTicker()));
            return null;
        }

        //Не открывать ордер если он уже есть такого же объёма
        if (strategyTv.getQuantity().compareTo(changingStrategyTv.getCurrentPosition()) == 0) {
            if (strategyTv.getQuantity().doubleValue() == 0) {
                cancelOrders(changingStrategyTv.getTicker(), false, null);
                log.info(String.format("[%s]=> quantity:%s", CANCEL_CONDITIONAL_ORDERS, strategyTv.getQuantity()));
            }
            return null;
        }

        //REVERSE
        if ((strategyTv.getDirection().equals("buy") && changingStrategyTv.getCurrentPosition().doubleValue() > 0
                || strategyTv.getDirection().equals("sell") && changingStrategyTv.getCurrentPosition().doubleValue() < 0) && Boolean.TRUE.equals(changingStrategyTv.getDoReverse())) {
            if (changingStrategyTv.getCurrentPosition().doubleValue() != 0) {
                closeOpenOrders(changingStrategyTv, userCache);
                log.info(String.format("[%s]=> quantity:%s", CLOSE_OPEN_ORDERS_OR_REVERSE, strategyTv.getQuantity()));
            }
            return null;
        }

        if (strategyTv.getDirection().equals("buy")) {
            if (strategyTv.getQuantity().compareTo(changingStrategyTv.getCurrentPosition()) <= 0) {
                throw new OrderNotExecutedException(String.format("Неверный порядок ордеров, либо дублирование ордера: %s, %s!", "покупка", strategyTv.getQuantity()));
            }
            direction = OrderDirection.ORDER_DIRECTION_BUY;
        } else if (strategyTv.getDirection().equals("sell")) {
            if (strategyTv.getQuantity().compareTo(changingStrategyTv.getCurrentPosition()) >= 0) {
                throw new OrderNotExecutedException(String.format("Неверный порядок ордеров, либо дублирование ордера: %s, %s!", "продажа", strategyTv.getQuantity()));
            }
            direction = OrderDirection.ORDER_DIRECTION_SELL;
        }

        BigDecimal position = strategyTv.getQuantity().subtract(changingStrategyTv.getCurrentPosition());

        strategyTvList.set(Integer.parseInt(changingStrategyTv.getId()), changingStrategyTv);
        userCache.setStrategies(strategyTvList);
        USER_STORE.replace(strategyTv.getUserName(), userCache);

        Map<String, Object> map = ImmutableMap.of(
                "userCache", userCache,
                "strategyList", strategyTvList,
                "changingStrategy", changingStrategyTv,
                "position", position,
                "direction", direction,
                "orderId", orderLinkedId == null ? UUID.randomUUID().toString() : orderLinkedId,
                "isAmendOrder", false,
                "triggerPrice", Optional.ofNullable(strategyTv.getTriggerPrice())
        );

        return map;
    }

    @Override
    Object getPositionInfo() {
        return null;
    }

    public Map<String, Object> getPositionInfo(String symbol) {
        var positionListRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).settleCoin("USDT").symbol(symbol).build();
        return (Map<String, Object>) positionRestClient.getPositionInfo(positionListRequest);
    }

    public void getPositionInfoByPeriod() {
        telegramSignalList.removeIf(ticker -> {
            var res = CommonUtils.parseResponse(getPositionInfo(ticker), "size");
            if (res == null || Double.parseDouble(String.valueOf(res)) == 0) {
                cancelOrders(ticker, false, null);
                return true; // Удалить элемент
            }
            return false; // Оставить элемент
        });
    }

    void getTickersInfo() {
        var insProductInfoRequest = LendingDataRequest.builder().build();
        var insProductInfo = lendingRestClient.getInsProductInfo(insProductInfoRequest);
    }

    void getAccountInfo() {
        var walletBalanceRequest = AccountDataRequest.builder().accountType(AccountType.UNIFIED).coin("USDT").build();
        var info = ((Map<String, Object>) (accountClient.getAccountInfo()));
        var data = ((Map<String, Object>) (accountClient.getWalletBalance(walletBalanceRequest)));
    }

    LinkedHashMap<String, Object> sendOrder(OrderDirection direction, String position, String ticker, String orderId, String triggerPrice, Boolean isAmendOrder, StrategyOptions options, BigDecimal executionPrice, String name, String comment, BigDecimal currentPosition, BigDecimal tpl) {
        TradeOrderRequest tradeOrderRequest = null;
        LinkedHashMap<String, Object> response = null;
        List<ConditionalOrder> list = ORDERS_MAP.get(name);
        if (CollectionUtils.isEmpty(list)) {
            list = new ArrayList<>();
            ORDERS_MAP.put(name, list);
        }

        if ((currentPosition != null && currentPosition.doubleValue() != 0) && tpl != null) {
            // Закрыть ордер с возможностью перестановки
            if (java.util.Objects.equals(tpl, BigDecimal.ZERO)) {
                deleteOrReplaceConditionalOrders(name, ticker, true, null, null);
            } else {
                // Выставить ордер с возможностью перестановки (например трейлинг стоп)
                deleteOrReplaceConditionalOrders(name, ticker, false, currentPosition, tpl);
            }
        }

        if (options.getUseGrid() && comment != null && comment.contains("grid")) {
            if (CommonUtils.getBigDecimal(position).doubleValue() == 0) {
                cancelOrders(ticker, false, null);
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
                return null;
            }
            if (triggerPrice == null) {
                modifyOrdersMap(orderId, name, null, null, false);
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
                gridOrders(direction, ticker, options, executionPrice, name, false, null, null);
            } else {
                gridOrders(direction, ticker, options, getBigDecimal(triggerPrice), name, true, currentPosition, CommonUtils.getBigDecimal(position));
            }
        }
        if (comment == null || !comment.contains("grid")) {
            if (triggerPrice == null) {
                modifyOrdersMap(orderId, name, null, null, false);
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
                    modifyOrdersMap(orderId, name, null, null, false);
                    //trigerPrice!=null
                    cancelOrders(ticker, true, null);
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

//                    if (options.getUseGrid() && comment != null && comment.contains("grid")) {
//                        gridOrders(direction, ticker, options, executionPrice, name, false, null);
//                    }
                }
            }
        }
        if (response == null) {
            return response;
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
        List<StrategyTv> strategyTvList = userCache.getStrategies();
        if (CollectionUtils.isEmpty(strategyTvList)) {
            return List.of("tickers.BTCUSDT");
        }
        return strategyTvList.stream().map(i -> String.format("tickers.%s", i.getTicker())).collect(Collectors.toList());
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
                                    String orderId = (String) map.get("orderId");
                                    setCurrentPosition(null, orderLinkId, execPrice, null, side, execQty, null, null, orderId);
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
        String signature = HmacSHA256Signer.auth(val, serial);

        var args = List.of(key, expires, signature);
        var authMap = Map.of("req_id", generateTransferID(), "op", "auth", "args", args);
        return JSON.toJSONString(authMap);
    }

    public static Map<String, List<ConditionalOrder>> getOrdersMap() {
        return ORDERS_MAP;
    }

    public Map<String, Object> getOpenOrders(String symbol) {
        TradeOrderRequest tradeOrderRequest = TradeOrderRequest.builder()
                .category(CategoryType.LINEAR)
                .symbol(symbol)
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
                    List<StrategyTv> strategyTvList = userCache.getStrategies();
                    if (!CollectionUtils.isEmpty(strategyTvList)) {
                        Optional<StrategyTv> execStrategy = strategyTvList
                                .stream()
                                .filter(item -> item.getFigi().equals(symbol)).findFirst();
                        if (execStrategy.isPresent()) {
                            var exec = execStrategy.get();
                            if (exec.getCurrentPosition().compareTo(size) != 0) {
                                exec.setCurrentPosition(size);
                                userCache.setStrategies(strategyTvList);
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

    private void deleteOrReplaceConditionalOrders(String name, String ticker, Boolean doRemove, BigDecimal currentPosition, BigDecimal tpl) {
        List<ConditionalOrder> list = ORDERS_MAP.get(name);
        Iterator<ConditionalOrder> iterator = list.iterator();
        while (iterator.hasNext()) {
            ConditionalOrder i = iterator.next();
            if (i != null) {
                if (i.getCanReplace()) {
                    if (doRemove) {
                        cancelOrders(ticker, false, i.getOrderLinkId());
                        iterator.remove();
                    } else {
                        cancelOrders(ticker, false, i.getOrderLinkId());
                        iterator.remove();
                    }
                }
            }
        }
        if (!doRemove) {
            var orderId = UUID.randomUUID().toString();
            modifyOrdersMap(orderId, name, null, null, true);
            var response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .symbol(ticker)
                    .orderType(TradeOrderType.MARKET)
                    .orderLinkId(orderId)
                    .triggerPrice(tpl.setScale(1, RoundingMode.HALF_UP).toString())
                    .tpTriggerBy(TriggerBy.LAST_PRICE)
                    .qty(String.valueOf(currentPosition))
                    .triggerDirection(currentPosition.doubleValue() > 0 ? 2 : 1)
                    .side(currentPosition.doubleValue() > 0 ? Side.SELL : Side.BUY)
                    .build());

            var retCode = response.get("retCode");
            if (!Objects.equal(retCode, 0)) {
                var message = response.get("retMsg");
                throw new OrderNotExecutedException(String.format("Ошибка выставления тейк профитного ордера. Message: %s.", message));
            }
        }
    }

    // Закрытие условных ордеров
    public void cancelOrders(String ticker, boolean all, String orderId) {
        TradeOrderRequest tradeOrderRequest = null;
        LinkedHashMap<String, Object> response = null;
        if (all) {
            tradeOrderRequest = TradeOrderRequest.builder()
                    .symbol(ticker)
                    .category(CategoryType.LINEAR)
                    .build();
            response = (LinkedHashMap<String, Object>) orderRestClient.cancelAllOrder(tradeOrderRequest);
        } else {
            if(orderId==null){
                return;
            }
            tradeOrderRequest = TradeOrderRequest.builder()
                    .symbol(ticker)
                    .orderLinkId(orderId)
                    .category(CategoryType.LINEAR)
                    .build();
            response = (LinkedHashMap<String, Object>) orderRestClient.cancelOrder(tradeOrderRequest);
        }

        var retCode = response.get("retCode");
        if (!Objects.equal(retCode, 0)) {
            var message = response.get("retMsg");
//            throw new OrderNotExecutedException(String.format("Ошибка отмены ордеров. Message: %s.", message));
        }
    }

    private void gridOrders(OrderDirection direction, String ticker, StrategyOptions options, BigDecimal price, String name, boolean beginFirst, BigDecimal currentPosition, BigDecimal position) {
        List<ConditionalOrder> list = ORDERS_MAP.get(name);

        //Если мы в длинной позиции то не трогаем уловные ордера на покупку и оредра tpl -> перевыставляем только ордера на продажу
        if (direction==OrderDirection.ORDER_DIRECTION_SELL) {
            Iterator<ConditionalOrder> iterator = list.iterator();
            while (iterator.hasNext()) {
                ConditionalOrder i = iterator.next();
                if (i.getDirection() == OrderDirection.ORDER_DIRECTION_SELL && java.util.Objects.equals(i.getCanReplace(), Boolean.FALSE)) {
                    cancelOrders(ticker, false, i.getOrderLinkId());
                    iterator.remove();
                }
            }
        }

        if (direction == OrderDirection.ORDER_DIRECTION_BUY) {
            Iterator<ConditionalOrder> iterator = list.iterator();
            while (iterator.hasNext()) {
                ConditionalOrder i = iterator.next();
                if (i.getDirection() == OrderDirection.ORDER_DIRECTION_BUY && java.util.Objects.equals(i.getCanReplace(), Boolean.FALSE)) {
                    cancelOrders(ticker, false, i.getOrderLinkId());
                    iterator.remove();
                }
            }
        }

       //удалить все условные ордера по инструменту
//        if (currentPosition == null || currentPosition.doubleValue() == 0) {
//            cancelOrders(ticker, true, null);
//            list.clear();
//        }

        var countGrids = options.getCountOfGrid();
        for (int i = 0; i < countGrids; i++) {
            if (beginFirst && i == 0) {
                price = price;
            } else {
                price = direction == OrderDirection.ORDER_DIRECTION_BUY ? price.multiply(BigDecimal.valueOf(100L).add(options.getOffsetOfGrid()).divide(BigDecimal.valueOf(100L))) : price.multiply(BigDecimal.valueOf(100L).subtract(options.getOffsetOfGrid()).divide(BigDecimal.valueOf(100L)));
            }
//            if ((currentPosition == null || currentPosition.doubleValue() != 0.0d) && i == 0 && beginFirst) {
//                continue;
//            }
            var idOrder = UUID.randomUUID().toString();
            modifyOrdersMap(idOrder, name, direction, null, false);
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

    private void closeOpenOrders(StrategyTv changingStrategyTv, UserCache userCache) {
        List<ConditionalOrder> list = ORDERS_MAP.get(changingStrategyTv.getName());
        if (CollectionUtils.isEmpty(list)) {
            list = new ArrayList<>();
            ORDERS_MAP.put(changingStrategyTv.getName(), list);
        }

        Side direction = null;
        if (changingStrategyTv.getCurrentPosition().doubleValue() > 0) {
            direction = Side.SELL;
        } else {
            direction = Side.BUY;
        }

        var tradeOrderRequest = TradeOrderRequest.builder()
                .category(CategoryType.LINEAR)
                .symbol(changingStrategyTv.getTicker())
                .side(direction)
                .orderType(TradeOrderType.MARKET)
                .orderLinkId(UUID.randomUUID().toString())
                .qty(String.valueOf(Math.abs(changingStrategyTv.getCurrentPosition().doubleValue())))
                .build();

        var response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
        var strategyList = userCache.getStrategies();
        var strategy = strategyList.stream().filter(i -> i.getId().equals(changingStrategyTv.getId())).findFirst().get();
        if (!Objects.equal(response.get("retCode"), 0)) {
            var message = response.get("retMsg");
            var errorData = new ErrorData();
            errorData.setMessage(String.format("Ошибка закрытия открытого ордера. Message: %s.", message));
            errorData.setTime(DateUtils.getCurrentTime());
            strategyList.stream().forEach(i -> {
                if (i.getId().equals(changingStrategyTv.getId())) {
                    i.setErrorData(errorData);
                }
            });
            userCache.setStrategies(strategyList);
            USER_STORE.replace("Admin", userCache);
            sendMessageInSocket(userCache.getStrategies());
            return;
        }

        strategyList.stream().forEach(i -> {
            if (i.getId().equals(changingStrategyTv.getId())) {
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
//                     Double.valueOf((String) leverageFilter.get("maxLeverage")) >= 25d
                    if (settleCoin.equals("USDT")) {
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

    public void getClosedPnlForTicker(String orderIds, StrategyTv changingStrategyTv) {
        Comparator comparator = new Comparator<Pnl>() {
            @Override
            public int compare(Pnl o1, Pnl o2) {
                if (o1.getTime() == o2.getTime()) {
                    return 0;
                }
                if (o1.getTime() > o2.getTime()) {
                    return 1;
                }
                return -1;
            }
        };

        Set<Pnl> pnlList = new TreeSet<>(comparator);

        LocalDateTime start = Instant.ofEpochMilli(changingStrategyTv.getCreatedDate()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        long startOfDay = start.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        var closPnlRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).startTime(startOfDay).limit(100).build();
        var response = (LinkedHashMap<String, Object>) positionRestClient.getClosePnlList(closPnlRequest);
        var result = (LinkedHashMap<String, Object>) response.get("result");
        if (!response.get("retCode").equals(0)) {
            log.info(String.format("[%s]=> message:%s", ERROR, response.get("message")));
            setErrorAndSetOnUi("Ошибка загрзуки");
        }
        var data = (List) result.get("list");
        if (data != null && data.size() > 0) {
            data.forEach(it -> {
                Map<String, Object> map = ((Map<String, Object>) it);
                var symbol = (String) map.get("symbol");
                var orderId = (String) map.get("orderId");
                if (orderIds.equals(orderId)) {
                    var closedPnl = getBigDecimal((String) map.get("closedPnl"));
                    var updatedTime = Long.valueOf((String) map.get("updatedTime"));
                    var size = (String) map.get("closedSize");
                    pnlList.add(new Pnl(symbol, closedPnl, orderId, updatedTime, BigDecimal.ZERO, size));
                }
            });
        }
        changingStrategyTv.getClosedPnl().clear();
        changingStrategyTv.getClosedPnl().addAll(pnlList);
    }

    public Map<String, Set<Pnl>> getClosedPnl(Long date) {
        Comparator comparator = new Comparator<Pnl>() {
            @Override
            public int compare(Pnl o1, Pnl o2) {
                if (o1.getTime() == o2.getTime()) {
                    return 0;
                }
                if (o1.getTime() > o2.getTime()) {
                    return 1;
                }
                return -1;
            }
        };

        Set<Pnl> pnlList = new TreeSet<>(comparator);
        Map<String, Pnl> feeMap = new HashMap<>();

        int plusDay = 0;
        LocalDateTime start = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDateTime();
        long startOfDay = start.toLocalDate().plusDays(plusDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long midddleDay = start.plusDays(plusDay + 1).toLocalDate().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endOfDay = LocalDateTime.now().plusDays(1).toLocalDate().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        do {
            var tradeFee = PositionDataRequest.builder().category(CategoryType.LINEAR).startTime(startOfDay).endTime(midddleDay).limit(500).build();
            var response = (LinkedHashMap<String, Object>) positionRestClient.getExecutionList(tradeFee);
            if (!response.get("retCode").equals(0)) {
                log.info(String.format("[%s]=> message:%s", ERROR, response.get("message")));
                setErrorAndSetOnUi("Ошибка загрзуки");
            }
            var result = (LinkedHashMap<String, Object>) response.get("result");
            var nextPageCursor = (String) result.get("nextPageCursor");
            var data = (List) result.get("list");
            if (data != null && data.size() > 0) {
                data.forEach(it -> {
                    Map<String, Object> map = ((Map<String, Object>) it);
                    var symbol = (String) map.get("symbol");
                    var orderId = (String) map.get("orderId");
                    var fee = getBigDecimal((String) map.get("execFee"));
                    var time = Long.valueOf((String) map.get("execTime"));
                    feeMap.put(orderId, new Pnl(symbol, null, orderId, time, fee, null));
                });
            }
            plusDay++;
            startOfDay = start.toLocalDate().plusDays(plusDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            midddleDay = start.plusDays(plusDay).toLocalDate().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } while (midddleDay != endOfDay);

        plusDay = 0;
        startOfDay = start.toLocalDate().plusDays(plusDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        midddleDay = start.plusDays(plusDay + 1).toLocalDate().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        do {
            var closPnlRequest = PositionDataRequest.builder().category(CategoryType.LINEAR).startTime(startOfDay).endTime(midddleDay).limit(500).build();
            var response = (LinkedHashMap<String, Object>) positionRestClient.getClosePnlList(closPnlRequest);
            var result = (LinkedHashMap<String, Object>) response.get("result");
            if (!response.get("retCode").equals(0)) {
                log.info(String.format("[%s]=> message:%s", ERROR, response.get("message")));
                setErrorAndSetOnUi("Ошибка загрзуки");
            }
            var nextPageCursor = (String) result.get("nextPageCursor");
            var data = (List) result.get("list");
            if (data != null && data.size() > 0) {
                data.forEach(it -> {
                    Map<String, Object> map = ((Map<String, Object>) it);
                    var symbol = (String) map.get("symbol");
                    var orderId = (String) map.get("orderId");
                    var closedPnl = getBigDecimal((String) map.get("closedPnl"));
                    var updatedTime = Long.valueOf((String) map.get("updatedTime"));
                    var fee = feeMap.get(orderId) != null ? feeMap.get(orderId).getFee() : BigDecimal.ZERO;
                    var size = (String) map.get("closedSize");
                    pnlList.add(new Pnl(symbol, closedPnl, orderId, updatedTime, fee, size));
                });
            }
            plusDay++;
            startOfDay = start.toLocalDate().plusDays(plusDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            midddleDay = start.plusDays(plusDay).toLocalDate().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } while (midddleDay != endOfDay);

        Map<String, Set<Pnl>> map = new HashMap<>();
        UserCache userCache = USER_STORE.get("Admin");
        List<StrategyTv> strategyTvList = userCache.getStrategies();
        map.put("COMMON", pnlList);
        Map<String, Set<Pnl>> groupedBySymbol = pnlList.stream().collect(Collectors.groupingBy(Pnl::getSymbol, Collectors.toCollection(HashSet::new)));
        map.putAll(groupedBySymbol);

        return map;
    }

    public TestTelegramChannel getMarketDataForTelegramTest(TelegramSignal telegramSignal) throws InterruptedException, IOException {
        var positionFix = false;
        LocalDate date = Instant.ofEpochMilli(telegramSignal.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // Получаем начало дня (00:00:00) в миллисекундах
        LocalDateTime startOfDay = date.atStartOfDay();
        var start = startOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        var end = start + 54000000L;
        var finalPeriod = Instant.now().toEpochMilli();
        //var finalPeriod=Instant.ofEpochMilli(start).plus(14, ChronoUnit.DAYS).toEpochMilli();
        Set<Candle> candles = new TreeSet<>(new Comparator<Candle>() {
            @Override
            public int compare(Candle c1, Candle c2) {
                try {
                    // Предполагаем, что getTime() возвращает объект, который реализует Comparable
                    return c1.getTime().compareTo(c2.getTime());
                } catch (NullPointerException e) {
                    // Обработка случая, когда один из объектов null
                    // Например, если c1.getTime() или c2.getTime() возвращает null
                    if (c1.getTime() == null && c2.getTime() == null) {
                        return 0; // Оба null считаем равными
                    } else if (c1.getTime() == null) {
                        return 1; // Null считается "больше" любого не-null значения
                    } else {
                        return -1; // Не-null считается "меньше" null
                    }
                } catch (Exception e) {
                    // Обработка других возможных исключений
                    e.printStackTrace(); // Логируем исключение
                    return 0; // Можно вернуть 0, чтобы считать элементы равными в случае ошибки
                }
            }
        });// Пул потоков

        int count = 0;
        final int[] countSuccess = {0};

        try {
            Ticker ticker = null;
            try {
                ticker = getFigi(List.of(telegramSignal.getSymbol()));
            } catch (Exception e) {
                return new TestTelegramChannel(null, null, null, null, 0, null, null, 0, false, null, null, null);
            }
            var fromStore = CANDLE_TEST.get(telegramSignal.getSymbol() + telegramSignal.getTime());

            while (end < finalPeriod) {
                try {
                    if (!CollectionUtils.isEmpty(fromStore)) {
                        candles.addAll(fromStore);
                        break;
                    }
                } catch (Exception e) {
                    return new TestTelegramChannel(null, null, null, null, 0, null, null, 0, false, null, null, null);
                }
                var marketKLineRequest = MarketDataRequest.builder()
                        .category(CategoryType.LINEAR)
                        .symbol(telegramSignal.getSymbol())
                        .marketInterval(MarketInterval.ONE_MINUTE)
                        .limit(1000)
                        .start(start)
                        .end(end)
                        .build();

                count++;
                dataRestClient.getMarketLinesData(marketKLineRequest, new BybitApiCallback<Object>() {
                    @Override
                    public void onResponse(Object response) {
                        if (response != null) {
                            var res = (LinkedHashMap<String, Object>) response;
                            if (res != null) {
                                var result = (LinkedHashMap<String, Object>) res.get("result");
                                if (result != null) {
                                    var list = (List) result.get("list");
                                    if (!CollectionUtils.isEmpty(list)) {
                                        list.forEach(item -> {
                                            if (item != null) {
                                                var i = (List) item;
                                                if (!CollectionUtils.isEmpty(i)) {
                                                    // Добавляем в основную коллекцию
                                                    try {
                                                        candles.add(new Candle(Long.valueOf((String) i.get(0)),
                                                                CommonUtils.getBigDecimal((String) i.get(1)),
                                                                CommonUtils.getBigDecimal((String) i.get(2)),
                                                                CommonUtils.getBigDecimal((String) i.get(3)),
                                                                CommonUtils.getBigDecimal((String) i.get(4))));
                                                    } catch (Exception ignored) {
                                                    }
                                                }
                                            }
                                        });

                                    }
                                }
                            }
                        }
                        countSuccess[0]++;
                    }

                    @Override
                    public void onFailure(Throwable cause) {
                        countSuccess[0]++;
                    }
                });

                if (count == 300) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                start = end;
                end = start + 54000000L;
            }

            int prevCount = 0;
            int countPrevCount = 0;
            while (countSuccess[0] != count) {
                if (prevCount == countSuccess[0]) {
                    countPrevCount++;
                } else {
                    countPrevCount = 0;
                    prevCount = countSuccess[0];
                }
                if (countPrevCount >= 5) {
                    if (Math.abs(countSuccess[0] - count) < 3) {
                        break;
                    } else {
                        return new TestTelegramChannel(null, null, null, null, 0, null, null, 0, false, null, null, null);
                    }
                }
                Thread.sleep(5000);
                log.debug(telegramSignal.getSymbol() + ":" + countPrevCount);
            }
            if (CollectionUtils.isEmpty(fromStore)) {
                CANDLE_TEST.put(telegramSignal.getSymbol() + telegramSignal.getTime(), candles);
                if (CANDLE_TEST.size() > 0) {
//                       saveToFile(CANDLE_TEST);
                }
            }
            var begin = telegramSignal.getTime();
            BigDecimal open = null;
            BigDecimal averageOpenPrice = null;
            BigDecimal tp1 = CommonUtils.getBigDecimal(telegramSignal.getTake1());
            BigDecimal tp2 = CommonUtils.getBigDecimal(telegramSignal.getTake2());
            BigDecimal tp3 = CommonUtils.getBigDecimal(telegramSignal.getTake3());
            BigDecimal stop = telegramSignal.getStop() != null ? CommonUtils.getBigDecimal(telegramSignal.getStop()) : null;
            BigDecimal profit = BigDecimal.ZERO;
            var entry1 = CommonUtils.getBigDecimal(telegramSignal.getEntry1());
            var entry2 = CommonUtils.getBigDecimal(telegramSignal.getEntry2());
            Boolean useTrp1 = false;
            Boolean useTrp2 = false;
            Boolean useTrp3 = false;
            BigDecimal trp1 = null;
            BigDecimal trp2 = null;
            BigDecimal trp3 = null;
            var fixTp1 = false;
            var fixTp2 = false;
            var fixTp3 = false;
            var increaseValue = CommonUtils.getBigDecimal(telegramSignal.getIncreaseValue());
            var fixValue1 = CommonUtils.getBigDecimal(telegramSignal.getTakeValue1());
            var fixValue2 = CommonUtils.getBigDecimal(telegramSignal.getTakeValue2());
            var countAdd = 0;
            var countFix = 0;
            if (entry1 != null && entry2 != null) {
                var delemiter = Math.abs(entry1.subtract(entry2).divide(BigDecimal.valueOf(3L), RoundingMode.HALF_EVEN).doubleValue());
                trp1 = BigDecimal.valueOf(telegramSignal.getDirection().equals("buy") ? entry1.doubleValue() - 1 * delemiter : entry1.doubleValue() + 1 * delemiter);
                trp2 = BigDecimal.valueOf(telegramSignal.getDirection().equals("buy") ? entry1.doubleValue() - 2 * delemiter : entry1.doubleValue() + 2 * delemiter);
                trp3 = BigDecimal.valueOf(telegramSignal.getDirection().equals("buy") ? entry1.doubleValue() - 3 * delemiter : entry1.doubleValue() + 3 * delemiter);
            } else {
                useTrp1 = null;
                useTrp2 = null;
                useTrp3 = null;
            }
            var direction = !telegramSignal.getUseReverse() ? telegramSignal.getDirection() : (telegramSignal.getDirection().equals("buy") ? "sell" : "buy");
            Iterator<Candle> iterator = candles.iterator();
            Long closeTime = null;
            BigDecimal closePrice = null;
            var minLot = ticker.getMinLot().doubleValue();
            var source = (entry2 != null ? entry2.doubleValue() : entry1 != null ? entry1.doubleValue() : stop.doubleValue());
            var beginQuanity = BigDecimal.valueOf(Math.ceil(100.0d / source / minLot)).multiply(ticker.getMinLot());
            if (telegramSignal.getSymbol().equals("AVAXUSDT")) {
                System.out.println();
            }
            var quantity = beginQuanity;
            var closeQuantity = BigDecimal.ZERO;
            var fixByStop = false;
            var drowdawn = BigDecimal.ZERO;
            Candle lastCandle = null;
            int countx = 0;
            while (iterator.hasNext()) {
                if ((countx - candles.size()) >= 10000) {
                    break;
                }
                countx++;
                log.debug("Countx: " + countx + telegramSignal.getSymbol());
                var candle = iterator.next();
                var cond1 = candle.getTime().compareTo(begin) == 0;
                var cond2 = entry1 != null ? ((candle.getTime().compareTo(begin) >= 0d) && (direction.equals("buy") ? candle.getLow().compareTo(entry1) <= 0 : candle.getHigh().compareTo(entry1) >= 0)) : false;
                if (open == null && (telegramSignal.getMode() == 1 ? cond1 : cond2)) {
                    open = candle.getClose();
                    averageOpenPrice = open;
                    //********//
                    if (telegramSignal.getUseReverse()) {
                        BigDecimal diff = null;
                        if (stop != null) {
                            diff = BigDecimal.valueOf(Math.abs(open.subtract(stop).doubleValue()));
                            if (telegramSignal.getDirection().equals("buy")) {
                                stop = open.add(diff);
                            } else {
                                stop = open.subtract(diff);
                            }
                        }
                        if (tp1 != null) {
                            diff = BigDecimal.valueOf(Math.abs(open.subtract(tp1).doubleValue()));
                            if (telegramSignal.getDirection().equals("buy")) {
                                tp1 = open.subtract(diff);
                            } else {
                                tp1 = open.add(diff);
                            }
                        }
                        if (tp2 != null) {
                            diff = BigDecimal.valueOf(Math.abs(open.subtract(tp2).doubleValue()));
                            if (telegramSignal.getDirection().equals("buy")) {
                                tp2 = open.subtract(diff);
                            } else {
                                tp2 = open.add(diff);
                            }
                        }
                        if (tp3 != null) {
                            diff = BigDecimal.valueOf(Math.abs(open.subtract(tp3).doubleValue()));
                            if (telegramSignal.getDirection().equals("buy")) {
                                tp3 = open.subtract(diff);
                            } else {
                                tp3 = open.add(diff);
                            }
                        }
                        if (entry1 != null) {
                            diff = BigDecimal.valueOf(Math.abs(open.subtract(entry1).doubleValue()));
                            if (telegramSignal.getDirection().equals("buy")) {
                                entry1 = open.add(diff);
                            } else {
                                entry1 = open.subtract(diff);
                            }
                        }
                        if (entry2 != null) {
                            diff = BigDecimal.valueOf(Math.abs(open.subtract(entry2).doubleValue()));
                            if (telegramSignal.getDirection().equals("buy")) {
                                entry2 = open.add(diff);
                            } else {
                                entry2 = open.subtract(diff);
                            }
                        }
                        if (entry1 != null && entry2 != null) {
                            var delemiter = Math.abs(entry1.subtract(entry2).divide(BigDecimal.valueOf(3L), RoundingMode.HALF_EVEN).doubleValue());
                            trp1 = BigDecimal.valueOf(direction.equals("buy") ? entry1.doubleValue() - 1 * delemiter : entry1.doubleValue() + 1 * delemiter);
                            trp2 = BigDecimal.valueOf(direction.equals("buy") ? entry1.doubleValue() - 2 * delemiter : entry1.doubleValue() + 2 * delemiter);
                            trp3 = BigDecimal.valueOf(direction.equals("buy") ? entry1.doubleValue() - 3 * delemiter : entry1.doubleValue() + 3 * delemiter);
                        } else {
                            useTrp1 = null;
                            useTrp2 = null;
                            useTrp3 = null;
                        }
                    }
                    //********//
                    continue;
                }
                if (open != null) {
                    if (averageOpenPrice != null) {
                        var middleResult = (direction.equals("buy") ? lastCandle.getClose().subtract(averageOpenPrice).multiply(quantity) : averageOpenPrice.subtract(lastCandle.getClose()).multiply(quantity)).add(profit);
                        if (middleResult.compareTo(drowdawn) < 0) {
                            drowdawn = middleResult;
                        }
                    }
                    if (useTrp1 != null && !useTrp1 && ((candle.getLow().compareTo(trp1) <= 0 && direction.equals("buy")) || (candle.getHigh().compareTo(trp1) >= 0 && direction.equals("sell")))) {
                        countAdd++;
                        useTrp1 = true;
                        quantity = quantity.add(beginQuanity);
                        averageOpenPrice = open.add(trp1).divide(BigDecimal.valueOf(2.0d), RoundingMode.HALF_EVEN);
                    }
                    if (useTrp2 != null && !useTrp2 && ((candle.getLow().compareTo(trp2) <= 0 && direction.equals("buy")) || (candle.getHigh().compareTo(trp2) >= 0 && direction.equals("sell")))) {
                        countAdd++;
                        useTrp2 = true;
                        quantity = quantity.add(beginQuanity);
                        averageOpenPrice = open.add(trp1).add(trp2).divide(BigDecimal.valueOf(3.0d), RoundingMode.HALF_EVEN);
                    }
                    if (useTrp3 != null && !useTrp3 && ((candle.getLow().compareTo(trp3) <= 0 && direction.equals("buy")) || (candle.getHigh().compareTo(trp3) >= 0 && direction.equals("sell")))) {
                        countAdd++;
                        useTrp3 = true;
                        quantity = quantity.add(beginQuanity);
                        averageOpenPrice = open.add(trp1).add(trp2).add(trp3).divide(BigDecimal.valueOf(4.0d), RoundingMode.HALF_EVEN);
                    }

                    if (direction.equals("buy")) {
                        if (increaseValue.doubleValue() != 0 && candle.getHigh().subtract(averageOpenPrice).divide(averageOpenPrice, RoundingMode.HALF_EVEN).multiply(BigDecimal.valueOf(100.0d)).compareTo(increaseValue) >= 0) {
                            stop = averageOpenPrice;
                        }
                        if (tp1 != null && !fixTp1 && candle.getHigh().compareTo(tp1) >= 0) {
                            var cl = quantity.multiply(fixValue1);
                            closeQuantity = cl;
                            fixTp1 = true;
                            closeTime = candle.getTime();
                            closePrice = tp1;
                            profit = profit.add(tp1.subtract(averageOpenPrice).multiply(cl));
                            countFix++;
                            if (telegramSignal.getUseProfitLossPoint() && increaseValue.doubleValue() == 0) {
                                stop = averageOpenPrice;
                            }
                        }
                        if (closeQuantity.subtract(quantity).doubleValue() == 0) {
                            break;
                        }
                        if (tp2 != null && !fixTp2 && candle.getHigh().compareTo(tp2) >= 0) {
                            var cl = (quantity.subtract(closeQuantity)).multiply(fixValue2);
                            closeQuantity = closeQuantity.add(cl);
                            fixTp2 = true;
                            closeTime = candle.getTime();
                            closePrice = tp2;
                            profit = profit.add(tp2.subtract(averageOpenPrice).multiply(cl));
                            countFix++;
                        }
                        if (closeQuantity.subtract(quantity).doubleValue() == 0) {
                            break;
                        }
                        if (tp3 != null && !fixTp3 && candle.getHigh().compareTo(tp3) >= 0) {
                            var cl = quantity.subtract(closeQuantity);
                            closeQuantity = cl.add(closeQuantity);
                            fixTp3 = true;
                            closeTime = candle.getTime();
                            closePrice = tp3;
                            profit = profit.add(tp3.subtract(averageOpenPrice).multiply(cl));
                            countFix++;
                            break;
                        }
                        //******//
                        if (stop != null && candle.getLow().compareTo(stop) <= 0) {
                            var cl = quantity.subtract(closeQuantity);
                            closeQuantity = cl.add(closeQuantity);
                            profit = profit.add(stop.subtract(averageOpenPrice).multiply(cl));
                            closeTime = candle.getTime();
                            closePrice = candle.getClose();
                            fixByStop = true;
                            break;
                        }
                    } else {
                        if (increaseValue.doubleValue() != 0 && averageOpenPrice.subtract(candle.getLow()).divide(averageOpenPrice, RoundingMode.HALF_EVEN).multiply(BigDecimal.valueOf(100.0d)).compareTo(increaseValue) >= 0) {
                            stop = averageOpenPrice;
                        }
                        if (tp1 != null && !fixTp1 && candle.getLow().compareTo(tp1) <= 0) {
                            var cl = quantity.multiply(fixValue1);
                            closeQuantity = cl;
                            fixTp1 = true;
                            profit = profit.add(averageOpenPrice.subtract(tp1).multiply(cl));
                            closeTime = candle.getTime();
                            closePrice = tp1;
                            countFix++;
                            if (telegramSignal.getUseProfitLossPoint() && increaseValue.doubleValue() == 0) {
                                stop = averageOpenPrice;
                            }
                        }
                        if (closeQuantity.subtract(quantity).doubleValue() == 0) {
                            break;
                        }
                        if (tp2 != null && !fixTp2 && candle.getLow().compareTo(tp2) <= 0) {
                            var cl = (quantity.subtract(closeQuantity)).multiply(fixValue2);
                            closeQuantity = closeQuantity.add(cl);
                            fixTp2 = true;
                            profit = profit.add(averageOpenPrice.subtract(tp2).multiply(cl));
                            closeTime = candle.getTime();
                            closePrice = tp2;
                            countFix++;
                        }
                        if (closeQuantity.subtract(quantity).doubleValue() == 0) {
                            break;
                        }
                        if (tp3 != null && !fixTp3 && candle.getLow().compareTo(tp3) <= 0) {
                            var cl = quantity.subtract(closeQuantity);
                            closeQuantity = cl.add(closeQuantity);
                            fixTp3 = true;
                            profit = profit.add(averageOpenPrice.subtract(tp3).multiply(cl));
                            closeTime = candle.getTime();
                            closePrice = tp3;
                            countFix++;
                            break;
                        }
                        //******//
                        if (stop != null && candle.getHigh().compareTo(stop) >= 0) {
                            var cl = quantity.subtract(closeQuantity);
                            closeQuantity = cl.add(closeQuantity);
                            profit = profit.add(averageOpenPrice.subtract(stop).multiply(cl));
                            closeTime = candle.getTime();
                            closePrice = candle.getClose();
                            fixByStop = true;
                            break;
                        }
                    }
                }
                lastCandle = candle;
            }

            if (open == null) {
                return new TestTelegramChannel(null, null, null, null, 0, null, null, 0, false, null, null, null);
            }
            if (closeTime == null && closePrice == null) {
                profit = direction.equals("buy") ? lastCandle.getClose().subtract(averageOpenPrice).multiply(quantity) : averageOpenPrice.subtract(lastCandle.getClose()).multiply(quantity);
                positionFix = false;
            }
            if (closeQuantity.subtract(quantity).doubleValue() == 0) {
                positionFix = true;
            }

            var pnl = (profit.divide(BigDecimal.valueOf(100.0d).divide(BigDecimal.valueOf(40.0d), RoundingMode.HALF_EVEN), RoundingMode.HALF_EVEN).multiply(BigDecimal.valueOf(100.0d))).divide(BigDecimal.valueOf(countAdd + 1), RoundingMode.HALF_EVEN);
            return new TestTelegramChannel(open, profit, quantity, closeTime, countAdd, closePrice, averageOpenPrice, countFix, fixByStop, positionFix, pnl, drowdawn);
        } catch (Exception e) {
            throw e;
        }
    }

    public void saveToFile(Map<String, Set<Candle>> map) throws IOException {
        String x = mapper.writeValueAsString(map);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME, true))) {
            oos.writeObject(x);
            oos.flush();
        } catch (IOException e) {
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromFile() throws IOException, ClassNotFoundException {

        File file = new File(FILE_NAME);
        if (!file.exists() || file.length() == 0) {
            System.out.println("Файл не существует или пуст. Создается новая карта.");
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            CANDLE_TEST.putAll((Map<String, Set<Candle>>) ois.readObject());
        } catch (EOFException e) {
            // Эта ошибка может возникнуть, если файл был поврежден
            System.out.println("Файл поврежден или пуст. Создается новая карта.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}