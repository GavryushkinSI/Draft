package ru.app.draft.services;

import com.bybit.api.client.domain.CategoryType;
import com.bybit.api.client.domain.TradeOrderType;
import com.bybit.api.client.domain.TriggerBy;
import com.bybit.api.client.domain.trade.Side;
import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.app.draft.annotations.Audit;
import ru.app.draft.exception.OrderNotExecutedException;
import ru.app.draft.models.*;
import ru.app.draft.models.Order;
import ru.app.draft.store.Store;
import ru.app.draft.utils.CommonUtils;
import ru.app.draft.utils.DateUtils;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.prometheus.client.Counter.build;
import static ru.app.draft.models.EventLog.*;
import static ru.app.draft.store.Store.*;
import static ru.app.draft.utils.CommonUtils.getBigDecimal;

@Log4j2
@Component
public class ApiService extends AbstractApiService {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final MarketDataStreamService streamService;
    private final TelegramBotService telegramBotService;
    private static Boolean useSandbox;

    public ApiService(MarketDataStreamService streamService, TelegramBotService telegramBotService, @Value("${usesandbox}") Boolean useSandbox) {
        super(streamService);
        this.streamService = streamService;
        this.telegramBotService = telegramBotService;
        this.useSandbox = useSandbox;
    }

    public Ticker getFigi(List<String> tickers) {
        return TICKERS_TKS.get("tickers").stream().filter(i -> i.getValue().equals(tickers.get(0))).findFirst().get();
    }

    public void getAllTickers(InvestApi api, List<String> filter) {
        List<Ticker> tickers = api.getInstrumentsService().getTradableFuturesSync().stream().map(i -> new Ticker(i.getTicker(), i.getTicker(), i.getFigi(), i.getClassCode(), BigDecimal.valueOf(i.getLot()), null)).collect(Collectors.toList());
//        tickers.addAll(api.getInstrumentsService()
//                .getTradableSharesSync()
//                .stream()
//                .filter(i -> i.getClassCode().equals("TQBR"))
//                .map(i -> new Ticker(i.getTicker(), i.getTicker(), i.getFigi(), i.getClassCode(), BigDecimal.valueOf(i.getLot()), null))
//                .collect(Collectors.toList()));
//        tickers = tickers.stream().filter(i -> filter.contains(i.getValue())).collect(Collectors.toList());
        TICKERS_TKS.replace("tickers", tickers);
    }

    @Audit
    public void setSubscriptionOnCandle(InvestApi api, List<String> tickers) {
        StreamProcessor<MarketDataResponse> processor = (response) -> {
            if (response.hasTradingStatus()) {
                log.info("Новые данные по статусам: {}", response);
            } else if (response.hasPing()) {
                log.info("ping message");
            } else if (response.hasCandle()) {
//                Candle candle = response.getCandle();
//                String date = dateFormat.format(new Date(candle.getTime().getSeconds() * 1000));
//                CandleData candleData = new CandleData(date, List.of(candle.getOpen().getUnits(), candle.getHigh().getUnits(), candle.getLow().getUnits(), candle.getClose().getUnits()));
//                formatCandleOnPeriod(candleData, candle.getFigi());
//                log.info("Новая данные: {}", response);
            } else if (response.hasTrade()) {
//                log.info("Новые данные по сделкам: {}", response);
            } else if (response.hasLastPrice()) {
                LastPrice lastPrice = response.getLastPrice();
                BigDecimal price = lastPrice.getPrice().getNano() == 0 ? BigDecimal.valueOf(lastPrice.getPrice().getUnits(), 0) :
                        BigDecimal.valueOf(lastPrice.getPrice().getUnits()).add(BigDecimal.valueOf(lastPrice.getPrice().getNano(), 9));
                updateLastPrice(lastPrice.getFigi(), price, lastPrice.getTime());
            } else if (response.hasOrderbook()) {
                log.info("Новые данные по стакану: {}", response);
            } else if (response.hasSubscribeCandlesResponse()) {
                var successCount = response.getSubscribeCandlesResponse().getCandlesSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на свечи: {}", successCount);
                log.info("неудачных подписок на свечи: {}", errorCount);
            } else if (response.hasSubscribeInfoResponse()) {
                var successCount = response.getSubscribeInfoResponse().getInfoSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на статусы: {}", successCount);
                log.info("неудачных подписок на статусы: {}", errorCount);
            } else if (response.hasSubscribeOrderBookResponse()) {
                var successCount = response.getSubscribeOrderBookResponse().getOrderBookSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на стакан: {}", successCount);
                log.info("неудачных подписок на стакан: {}", errorCount);
            } else if (response.hasSubscribeTradesResponse()) {
                var successCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeTradesResponse().getTradeSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("удачных подписок на сделки: {}", successCount);
                log.info("неудачных подписок на сделки: {}", errorCount);
            } else if (response.hasSubscribeLastPriceResponse()) {
                var successCount = response.getSubscribeLastPriceResponse().getLastPriceSubscriptionsList().stream().filter(el -> el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                var errorCount = response.getSubscribeLastPriceResponse().getLastPriceSubscriptionsList().stream().filter(el -> !el.getSubscriptionStatus().equals(SubscriptionStatus.SUBSCRIPTION_STATUS_SUCCESS)).count();
                log.info("success subscribe on last price: {}", successCount);
                log.info("fail subscribe on last price: {}", errorCount);
            }
        };

        api.getMarketDataStreamService().newStream("stream", processor, message -> {
            try {
                throw new Exception(message);
            } catch (Exception e) {
                //List<String> figsList = new ArrayList<>();
                //TICKERS_TKS.get("tickers").forEach(i -> figsList.add(i.getFigi()));
                //this.setSubscriptionOnCandle(figsList);
            }
        }).subscribeLastPrices(tickers);
    }

    public void getHistoryByFigi(InvestApi api, List<String> figs) {
        figs.forEach((figi) ->
        {
            Instant from = Instant.parse("2022-12-20T00:00:00Z");
            Instant next = null;
            do {
                next = from.plus(24, ChronoUnit.HOURS);
                Instant to = next.isBefore(Instant.now()) ? next : Instant.now();
                api.getMarketDataService().getCandlesSync(figi, from, to, CandleInterval.CANDLE_INTERVAL_1_MIN).forEach(new Consumer<HistoricCandle>() {
                    @Override
                    public void accept(HistoricCandle item) {
                        String date = dateFormat.format(new Date(item.getTime().getSeconds() * 1000));
                        Store.addCandle(figi, new CandleData(date, List.of(item.getOpen().getUnits(), item.getHigh().getUnits(), item.getLow().getUnits(), item.getClose().getUnits())));
                    }
                });
                from = next;
            } while (from.isBefore(Instant.now()));
        });
    }

    @Audit
    private void formatCandleOnPeriod(CandleData candle, String figi) {
        if (!TEMP_STORE.containsKey(figi)) {
            TEMP_STORE.put(figi, candle);
            return;
        }
        if (TEMP_STORE.containsValue(candle)) {
            TEMP_STORE.compute(figi, (s, data) -> {
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
            CandleData newCandle = TEMP_STORE.get(figi);
            Store.CANDLE_STORE.computeIfPresent(figi, (s, data) -> {
                data.add(newCandle);
                Message message = new Message();
                message.setSenderName("server");
                message.setMessage(newCandle);
                message.setStatus(Status.JOIN);
                message.setCommand("newCandle");
//                Map<String, UserCache> map = Store.USER_STORE.entrySet().stream().filter(stringUserCacheEntry -> Objects.equals(stringUserCacheEntry.getValue().getTicker(), figi)).collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        Map.Entry::getValue
//                ));
//                streamService.sendDataToUser(map, message);
                return data;
            });
            TEMP_STORE.clear();
            TEMP_STORE.put(figi, candle);
        }
    }

    public void sendSignal(InvestApi api, StrategyTv strategyTv) {
        ErrorData errorData = null;
        StrategyTv changingStrategyTv = null;
        Map<String, Object> map = null;
        BigDecimal executionPrice = null;
        BigDecimal position = null;
        UserCache userCache = null;
        List<StrategyTv> strategyTvList = null;
        try {
            if(Boolean.FALSE.equals(strategyTv.getIsActive())){
                return;
            }
//            if (strategyTv.getPositionTv() != null) {
//                correctCurrentPosition(strategyTv);
//                return;
//            }
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
                var lastPrice=LAST_PRICE.get(changingStrategyTv.getFigi());
                Map<String, Object> result = sendOrderTKS(api, direction, CommonUtils.formatBigDecimalNumber(position), changingStrategyTv.getFigi(), ordeId, triggerPrice.map(String::valueOf).orElse(null), isAmendOrder, changingStrategyTv.getOptions(), lastPrice!=null?lastPrice.getPrice():null, changingStrategyTv.getName(), strategyTv.getComment(), changingStrategyTv.getCurrentPosition());
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
//        if (errorData != null) {
//            sendMessageInSocket(strategyTvList);
//        }
        if (changingStrategyTv.getConsumer().contains("test")) {
            String time = DateUtils.getCurrentTime();
            //updateStrategyCache(strategyTvList, strategyTv, changingStrategyTv, executionPrice, userCache, position, time, false, null);
        }
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
                        return it.getValue().stream().map(i->i.getOrderLinkId()).collect(Collectors.toList()).contains(orderLinkedId);
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
                    //updateStrategyCache(strategyTvList, execStrategyTv, execStrategyTv, executionPrice, userCache, execQty, DateUtils.getCurrentTime(), false, orderLinkedId);
                    //getPosition(execStrategyTv.getTicker(), false);
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
                //closeOpenOrders(changingStrategyTv, userCache);
            }
            return null;
        }

        //Закрываем ордеры которые не исполнены
        if (comment != null && comment.contains("cancel")) {
            //cancelOrders(changingStrategyTv.getTicker(), false, null);
            log.info(String.format("[%s]=> ticker:%s, close_condition_orders", CANCEL_CONDITIONAL_ORDERS, changingStrategyTv.getTicker()));
            return null;
        }

        //Не открывать ордер если он уже есть такого же объёма
        if (strategyTv.getQuantity().compareTo(changingStrategyTv.getCurrentPosition()) == 0) {
            if (strategyTv.getQuantity().doubleValue() == 0) {
                //cancelOrders(changingStrategyTv.getTicker(), false, null);
                log.info(String.format("[%s]=> quantity:%s", CANCEL_CONDITIONAL_ORDERS, strategyTv.getQuantity()));
            }
            return null;
        }

        //REVERSE
        if ((strategyTv.getDirection().equals("buy") && changingStrategyTv.getCurrentPosition().doubleValue() > 0
                || strategyTv.getDirection().equals("sell") && changingStrategyTv.getCurrentPosition().doubleValue() < 0) && Boolean.TRUE.equals(changingStrategyTv.getDoReverse())) {
            if (changingStrategyTv.getCurrentPosition().doubleValue() != 0) {
                //closeOpenOrders(changingStrategyTv, userCache);
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

    private LinkedHashMap<String, Object> sendOrderTKS(InvestApi api, OrderDirection direction, String position, String ticker, String orderId, String triggerPrice, Boolean isAmendOrder, StrategyOptions options, BigDecimal executionPrice, String name, String comment, BigDecimal currentPosition) {
        var accounts = api.getUserService().getAccountsSync();
        var triggerPr = CommonUtils.getBigDecimal(triggerPrice);
        var max= triggerPr.add(BigDecimal.valueOf(500));
        Quotation quotation = Quotation.newBuilder()
                .setUnits(triggerPr.longValue())
                .setNano(triggerPr.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(1_000_000_000)).intValue())
                .build();

        Quotation quotation2 = Quotation.newBuilder()
                .setUnits(max.longValue())
                .setNano(max.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(1_000_000_000)).intValue())
                .build();

        TradeOrderRequest tradeOrderRequest = null;
        LinkedHashMap<String, Object> response = null;
        List<ConditionalOrder> list = ORDERS_MAP.get(name);
        if (CollectionUtils.isEmpty(list)) {
            list = new ArrayList<>();
            ORDERS_MAP.put(name, list);
        }

        if (options.getUseGrid() && comment != null && comment.contains("grid")) {
            if (CommonUtils.getBigDecimal(position).doubleValue() == 0) {
                //cancelOrders(ticker, false, null);
                tradeOrderRequest = TradeOrderRequest.builder()
                        .category(CategoryType.LINEAR)
                        .symbol(ticker)
                        .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                        .orderType(TradeOrderType.MARKET)
                        .orderLinkId(orderId)
                        .qty(position)
                        .build();
                //response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
                var retCode3 = response.get("retCode");
                if (!com.google.common.base.Objects.equal(retCode3, 0)) {
                    var message = response.get("retMsg");
                    throw new OrderNotExecutedException(String.format("Ошибка исполнения рыночного ордера. Message: %s.", message));
                }
                return null;
            }
            if (triggerPrice == null) {
                tradeOrderRequest = TradeOrderRequest.builder()
                        .category(CategoryType.LINEAR)
                        .symbol(ticker)
                        .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                        .orderType(TradeOrderType.MARKET)
                        .orderLinkId(orderId)
                        .qty(position)
                        .build();
                //response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
                var retCode3 = response.get("retCode");
                if (!com.google.common.base.Objects.equal(retCode3, 0)) {
                    var message = response.get("retMsg");
                    throw new OrderNotExecutedException(String.format("Ошибка исполнения рыночного ордера. Message: %s.", message));
                }
                //gridOrders(direction, ticker, options, executionPrice, name, false, null, null);
            } else {
                //gridOrders(direction, ticker, options, getBigDecimal(triggerPrice), name, true, currentPosition, CommonUtils.getBigDecimal(position));
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
                //response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
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
                    //response = (LinkedHashMap<String, Object>) orderRestClient.amendOrder(tradeOrderRequest);
                } else {
                    modifyOrdersMap(orderId, name, null, null, false);
                    //trigerPrice!=null
                    //cancelOrders(ticker, false, null);
                    var string = api.getStopOrdersService().postStopOrderGoodTillCancel(
                            ticker,
                            Long.parseLong(position),
                            quotation2,
                            quotation,
                            direction == OrderDirection.ORDER_DIRECTION_BUY?StopOrderDirection.STOP_ORDER_DIRECTION_BUY:StopOrderDirection.STOP_ORDER_DIRECTION_SELL,
                            accounts.get(0).getId(),
                            StopOrderType.STOP_ORDER_TYPE_STOP_LIMIT
                    ).join();
                    //response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);

//                    if (options.getUseGrid() && comment != null && comment.contains("grid")) {
//                        gridOrders(direction, ticker, options, executionPrice, name, false, null);
//                    }
                }
            }
        }
        var retCode = response.get("retCode");

        if (com.google.common.base.Objects.equal(retCode, 110092) || com.google.common.base.Objects.equal(retCode, 110093)) {
            log.info(String.format("[%s]=> ticker:%s, triggerPrice:%s", ORDER_CANNOT_EXECUTE, ticker, triggerPrice));
            tradeOrderRequest = TradeOrderRequest.builder()
                    .category(CategoryType.LINEAR)
                    .symbol(ticker)
                    .side(direction == OrderDirection.ORDER_DIRECTION_BUY ? Side.BUY : Side.SELL)
                    .orderType(TradeOrderType.MARKET)
                    .orderLinkId(orderId)
                    .qty(position)
                    .build();
            //response = (LinkedHashMap<String, Object>) orderRestClient.createOrder(tradeOrderRequest);
            log.info(String.format("[%s]: symbol:%s, qty:%s", MARKET_ORDER_EXECUTE_FORCE, ticker, position));
        }
        var retCode2 = response.get("retCode");
        if (!com.google.common.base.Objects.equal(retCode2, 0) && !com.google.common.base.Objects.equal(retCode2, 110092) && !com.google.common.base.Objects.equal(retCode2, 110093)) {
            var message = response.get("retMsg");
            throw new OrderNotExecutedException(String.format("Ошибка исполнения ордера %s, %s, %s. Message: %s.", ticker, direction.name(), position, message));
        }
        return response;
    }

//    public void sendOrder(InvestApi api, StrategyTv strategyTv) {
//        //Выставляем заявку
//        UserCache userCache = USER_STORE.get(strategyTv.getUserName());
//        List<StrategyTv> strategyTvList = userCache.getStrategies();
//        StrategyTv changingStrategyTv = strategyTvList
//                .stream()
//                .filter(str -> str.getName().equals(strategyTv.getName())).findFirst().get();
//        if (!changingStrategyTv.getIsActive()) {
//            return;
//        }
//        BigDecimal position = BigDecimal.ZERO;
//        BigDecimal executionPrice = null;
//        String time = null;
//        ErrorData errorData = changingStrategyTv.getErrorData();
//        long commissions=0L;
//        OrderDirection direction = null;
//
//        if (changingStrategyTv.getConsumer().contains("terminal")) {
//            if (strategyTv.getDirection().equals("buy")) {
//                direction = OrderDirection.ORDER_DIRECTION_BUY;
//            } else if (strategyTv.getDirection().equals("sell")) {
//                direction = OrderDirection.ORDER_DIRECTION_SELL;
//            } else {
//                if (changingStrategyTv.getCurrentPosition().longValue()<0) {
//                    direction = OrderDirection.ORDER_DIRECTION_BUY;
//                } else {
//                    direction = OrderDirection.ORDER_DIRECTION_SELL;
//                }
//            }
//
//            var accounts = api.getUserService().getAccountsSync();
////            if (accounts.size() == 0) {
////                //Открываем счёт
////                String accountId = api.getSandboxService().openAccountSync();
////                //Пополнить счёт
////                api.getSandboxService().payIn(accountId, MoneyValue.newBuilder().setUnits(5000000).setCurrency("RUB").build());
////                accounts = api.getUserService().getAccountsSync();
////            }
//            var mainAccount = accounts.get(0).getId();
//            var price = Quotation.newBuilder().build();
//            position = strategyTv.getQuantity().subtract(changingStrategyTv.getCurrentPosition());
////            //Выставляем заявку на покупку по рыночной цене
//            PostOrderResponse orderResponse = null;
//            time = DateUtils.getCurrentTime();
//            try {
//                orderResponse = api.getOrdersService()
//                        .postOrderSync(
//                                changingStrategyTv.getFigi(),
//                                Math.abs(position.longValue()),
//                                price,
//                                direction,
//                                mainAccount,
//                                OrderType.ORDER_TYPE_MARKET,
//                                UUID.randomUUID().toString());
//            } catch (Exception e) {
//                errorData.setMessage(e.getMessage());
//                errorData.setTime(time);
//                return;
//            }
//            executionPrice = CommonUtils.getFromMoneyValue(orderResponse.getExecutedOrderPrice());
//            commissions = orderResponse.getExecutedCommission().getUnits();
//            if (useSandbox) {
//                Optional<PortfolioPosition> portfolioPosition = getPortfolio(api, mainAccount).getPositionsList().stream().filter(i -> i.getFigi().equals(changingStrategyTv.getFigi())).findFirst();
//
//                if (portfolioPosition.isPresent()) {
//                    var realQuantity = portfolioPosition.get().getQuantity();
//                    if (realQuantity.getUnits() != strategyTv.getQuantity().longValue()) {
//                        errorData.setMessage(String.format("Фактическое вол-во лотов:%s, планируемое:%s", realQuantity.getUnits(), strategyTv.getQuantity()));
//                        errorData.setTime(time);
//                        changingStrategyTv.setErrorData(errorData);
//                        return;
//                    }
//                } else {
//                    if (strategyTv.getQuantity().longValue() != 0) {
//                        errorData.setMessage(String.format("Фактическое вол-во лотов:%s, планируемое:%s", 0, strategyTv.getQuantity()));
//                        errorData.setTime(time);
//                        changingStrategyTv.setErrorData(errorData);
//                        return;
//                    }
//                }
//                if (!CollectionUtils.isEmpty(getOrders(api, mainAccount))) {
//                    errorData.setMessage("Есть неисполненные ордера!");
//                    errorData.setTime(time);
//                    return;
//                }
//            }
//        } else if (changingStrategyTv.getConsumer().contains("test")) {
//            executionPrice = LAST_PRICE.get(changingStrategyTv.getFigi()).getPrice();
//            position = strategyTv.getQuantity().subtract(changingStrategyTv.getCurrentPosition());
//        }
//        userCache.addCommission(commissions);
//        updateStrategyCache(strategyTvList, strategyTv, changingStrategyTv, executionPrice, userCache, position, time, null);
//        Message message = new Message();
//        message.setSenderName("server");
//        message.setMessage(userCache.getStrategies());
//        message.setCommand("strategy");
//        message.setStatus(Status.JOIN);
//        streamService.sendDataToUser(Set.of(strategyTv.getUserName()), message);
//    }

    private void updateStrategyCache(List<StrategyTv> strategyTvList, StrategyTv strategyTv, StrategyTv changingStrategyTv, BigDecimal executionPrice, UserCache userCache, BigDecimal position, String time, String orderLinkId) {
        var minLot = changingStrategyTv.getMinLot();
        String printPrice = CommonUtils.formatNumber(executionPrice, changingStrategyTv.getPriceScale());

        if (strategyTv.getDirection().equals(changingStrategyTv.getCurrentPosition().compareTo(BigDecimal.ZERO) > 0 ? "buy" : changingStrategyTv.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0 ? "sell" : "hold")) {
            changingStrategyTv.addEnterAveragePrice(executionPrice, false);
        } else {
            changingStrategyTv.addEnterAveragePrice(executionPrice, true);
        }

        if (strategyTv.getDirection().equals("buy")) {
            changingStrategyTv.setCurrentPosition(changingStrategyTv.getCurrentPosition().add(position));
            for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
                changingStrategyTv.addOrder(new Order(executionPrice, minLot, strategyTv.getDirection(), time, orderLinkId));
            }
            String text = String.format("%s => Покупка %s лотов по цене %s (priceTV:%s). Время %s.", strategyTv.getName(), Math.abs(position.doubleValue()), printPrice, strategyTv.getPriceTv(), time);
            userCache.addLogs(text);
            if (userCache.getUser().getChatId() != null && changingStrategyTv.getConsumer().contains("telegram")) {
                telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
            }
        }
        if (strategyTv.getDirection().equals("sell")) {
            changingStrategyTv.setCurrentPosition(changingStrategyTv.getCurrentPosition().add(position));
            for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
                changingStrategyTv.addOrder(new Order(executionPrice, minLot, strategyTv.getDirection(), time, orderLinkId));
            }
            String text = String.format("%s => Продажа %s лотов по цене %s (priceTV:%s). Время %s.", strategyTv.getName(), Math.abs(position.doubleValue()), printPrice, strategyTv.getPriceTv(), time);
            userCache.addLogs(text);
            if (userCache.getUser().getChatId() != null && changingStrategyTv.getConsumer().contains("telegram")) {
                telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
            }
        }
        if (strategyTv.getDirection().equals("hold")) {
            if (changingStrategyTv.getCurrentPosition().compareTo(BigDecimal.ZERO) != 0) {
                String text = String.format(changingStrategyTv.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0 ? "%s => Покупка %s лотов по цене %s (priceTV:%s). Время %s." : "%s => Продажа %s лотов по цене %s (priceTV:%s). Время %s.", strategyTv.getName(), Math.abs(position.doubleValue()), printPrice, strategyTv.getPriceTv(), time);
                userCache.addLogs(text);
                if (userCache.getUser().getChatId() != null && changingStrategyTv.getConsumer().contains("telegram")) {
                    telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
                }
                for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
                    changingStrategyTv.addOrder(new Order(executionPrice, minLot, changingStrategyTv.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0 ? "buy" : "sell", time, orderLinkId));
                }
                changingStrategyTv.setCurrentPosition(changingStrategyTv.getCurrentPosition().add(position));
            }
        }

        strategyTvList.set(Integer.parseInt(changingStrategyTv.getId()), changingStrategyTv);
        userCache.setStrategies(strategyTvList);
        USER_STORE.replace(strategyTv.getUserName(), userCache);
    }

    private static List<OrderState> getOrders(InvestApi api, String accountId) {
        return api.getOrdersService().getOrdersSync(accountId);
    }

    private static PortfolioResponse getPortfolio(InvestApi api, String accountId) {
        PortfolioResponse portfolioResponse = api.getSandboxService().getPortfolioSync(accountId);
        return portfolioResponse;
    }

    private static PositionsResponse getPosition(InvestApi api, String accountId) {
        PositionsResponse positionsResponse = api.getSandboxService().getPositionsSync(accountId);
        return positionsResponse;
    }

    private static void getOrderState(InvestApi api, String accountId, String orderId) {
        OrderState orderState = api.getSandboxService().getOrderStateSync(accountId, orderId);
        log.info(orderState);
    }

    private static void getOperations(InvestApi api, String accountId, String figi) {
        Instant from = Instant.now().minus(3, ChronoUnit.DAYS);
        Instant to = Instant.now();
        List<Operation> operations = api.getSandboxService().getOperationsSync(accountId, from, to, OperationState.OPERATION_STATE_EXECUTED, figi);
    }

    public List<AccountDto> getAccountInfo(InvestApi api, String userName) {
        UserCache userCache = USER_STORE.get(userName);
        AccountDto accountDto = new AccountDto();
        accountDto.setLogs(userCache.getLogs());
        List<Notification> notificationList = COMMON_INFO.get("Notifications");
        if (!userName.equals("Admin")) {
            notificationList = notificationList.stream().filter(i -> !i.getForAdmin()).collect(Collectors.toList());
        } else {
            if (useSandbox) {
                var accounts = api.getUserService().getAccountsSync();
                var mainAccount = accounts.get(0).getId();
                List<PortfolioPosition> portfolioPositions = getPortfolio(api, mainAccount).getPositionsList();
                portfolioPositions.forEach(i -> accountDto.addPortfolio(i.getFigi(), i.getQuantity().getUnits()));
            } else {
                accountDto.setPortfolio(userCache.getPortfolios());
            }
        }
        accountDto.setNotifications(notificationList.stream().limit(1000).collect(Collectors.toList()));
        accountDto.setTelegramSubscriptionExist(userCache.getUser().getChatId() != null);
        accountDto.setViewedNotifyIds(userCache.getUser().getViewedNotifyIds());

        return List.of(accountDto);
    }
}
