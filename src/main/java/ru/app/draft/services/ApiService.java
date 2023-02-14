package ru.app.draft.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import ru.app.draft.annotations.Audit;
import ru.app.draft.models.*;
import ru.app.draft.models.Order;
import ru.app.draft.store.Store;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.MarketDataSubscriptionService;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static ru.app.draft.store.Store.*;

@Log4j2
@Component
public class ApiService {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final MarketDataStreamService streamService;

    public volatile static long balance = 0L;

    public ApiService(MarketDataStreamService streamService) {
        this.streamService = streamService;
    }

    public List<String> getFigi(InvestApi api, List<String> tickers) {
        List<Future> list = api.getInstrumentsService().getAllFuturesSync();
        return list.stream().collect(ArrayList::new, (l, item) -> {
            if (tickers.contains(item.getTicker())) {
                l.add(item.getFigi());
            }
        }, ArrayList::addAll);
    }

    public List<Ticker> getAllTickers(InvestApi api) {
        List<Ticker> tickers = api.getInstrumentsService().getAllFuturesSync().stream().map(i -> new Ticker(i.getTicker(), i.getTicker(),i.getFigi(),i.getClassCode(), i.getLot())).collect(Collectors.toList());
        tickers.addAll(api.getInstrumentsService()
                .getAllSharesSync()
                .stream()
                .filter(i -> i.getClassCode().equals("TQBR"))
                .map(i -> new Ticker(i.getTicker(), i.getTicker(),i.getFigi(), i.getClassCode(), i.getLot()))
                .collect(Collectors.toList()));

        TICKERS.replace("tickers", tickers);
        return tickers;
    }

    @Audit
    public void setSubscriptionOnCandle(InvestApi api, List<String> figs) {
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
                //log.info("ticker " + lastPrice.getFigi() + " last price: " + lastPrice.getPrice().getUnits() + " " + new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date(lastPrice.getTime().getSeconds() * 1000)));
                updateLastPrice(lastPrice.getFigi(), lastPrice.getPrice().getUnits(), lastPrice.getTime());
//                Message message = new Message();
//                message.setSenderName("server");
//                message.setMessage(lastPrice.getPrice().getUnits());
//                message.setStatus(Status.JOIN);
//                message.setCommand("newLastPrice");
//                Map<String, UserCache> map = Store.USER_STORE.entrySet().stream().filter(stringUserCacheEntry -> Objects.equals(stringUserCacheEntry.getValue().getTicker(), lastPrice.getFigi())).collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        Map.Entry::getValue
//                ));
//                streamService.sendDataToUser(map, message);
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
                if (successCount > 0) {
                    COMMON_INFO.computeIfPresent("Notifications", (s, data) -> {
                        data.add(new Notification("Стрим подключен! Тикеры:" + figs, "info_success", Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")).getTime().toString()));
                        return data;
                    });
                }
                log.info("fail subscribe on last price: {}", errorCount);
            }
        };

        try {
            for (String itemFigs : figs) {
                api.getMarketDataStreamService().newStream(itemFigs, processor, message -> {
                    throw new RuntimeException(message);
                }).subscribeLastPrices(figs);
            }
        } catch (Exception e) {
            log.error(String.format("Error stream: %s", e.getMessage()));
            Map<String, MarketDataSubscriptionService> subscriptionServiceMap = api.getMarketDataStreamService().getAllStreams();
            COMMON_INFO.computeIfPresent("Notifications", (s, data) -> {
                data.add(new Notification("Стрим разорван: " + subscriptionServiceMap.size(), "error", Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")).getTime().toString()));
                List<String> reconnectStreamList = new ArrayList<>();
                LAST_PRICE.forEach((k, v) -> {
                    if (!subscriptionServiceMap.containsKey(k)) {
                        reconnectStreamList.add(k);
                    }
                });
                this.setSubscriptionOnCandle(api, reconnectStreamList);
                return data;
            });
        }
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

    public void sendOrder(InvestApi api, Strategy strategy) {
        //Выставляем заявку
//        var accounts = api.getUserService().getAccountsSync();
//        var mainAccountId = accounts.get(0).getId();
        var price = Quotation.newBuilder().setUnits(0L).setNano(0).build();
        var minLot = 1L;
        UserCache userCache = USER_STORE.get(strategy.getUserName());
        List<Strategy> strategyList = userCache.getStrategies();
        Strategy changingStrategy = strategyList
                .stream()
                .filter(str -> str.getName().equals(strategy.getName())).findFirst().get();
        if (changingStrategy.getConsumer().get(0).equals("test")) {
            Long v = LAST_PRICE.get(changingStrategy.getFigi()).getPrice();
//            v = Math.abs((long) new Random().nextInt(100));
            if (strategy.getDirection().equals("buy")) {
                long position = strategy.getQuantity() - changingStrategy.getCurrentPosition();
                changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition() + position);
                Date time = new Date();
                for (int i = 1; i <= Math.abs(position / minLot); i++) {
                    changingStrategy.addOrder(new Order(v, minLot, strategy.getDirection(), time));
                }
                userCache.addLogs(String.format("%s => Покупка %s лотов по цене %s (priceTV:%s). Время %s.", strategy.getName(), Math.abs(position), v, strategy.getPriceTv(), time));
            }
            if (strategy.getDirection().equals("sell")) {
                long position = strategy.getQuantity() - changingStrategy.getCurrentPosition();
                changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition() + position);
                Date time = new Date();
                for (int i = 1; i <= Math.abs(position / minLot); i++) {
                    changingStrategy.addOrder(new Order(v, minLot, strategy.getDirection(), time));
                }
                userCache.addLogs(String.format("%s => Продажа %s лотов по цене %s (priceTV:%s). Время %s.", strategy.getName(), Math.abs(position), v, strategy.getPriceTv(), time));
            }
            if (strategy.getDirection().equals("hold")) {
                if (changingStrategy.getCurrentPosition() != 0) {
                    long position = strategy.getQuantity() - changingStrategy.getCurrentPosition();
                    Date time = new Date();
                    userCache.addLogs(String.format(changingStrategy.getCurrentPosition() < 0 ? "%s => Покупка %s лотов по цене %s (priceTV:%s). Время %s." : "Стратегия %s=>Продажа %s лотов по цене %s (priceTV:%s). Время %s.", strategy.getName(), Math.abs(position), v, strategy.getPriceTv(), time));
                    for (int i = 1; i <= Math.abs(position / minLot); i++) {
                        changingStrategy.addOrder(new Order(v, minLot, changingStrategy.getCurrentPosition() < 0 ? "buy" : "sell", time));
                    }
                    changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition() + position);
                }
            }
            strategyList.set(Integer.parseInt(changingStrategy.getId()), changingStrategy);
            userCache.setStrategies(strategyList);
            USER_STORE.replace(strategy.getUserName(), userCache);
        } else {
            OrderDirection direction = strategy.getDirection().equals("buy") ? OrderDirection.ORDER_DIRECTION_BUY :
                    strategy.getDirection().equals("sell") ? OrderDirection.ORDER_DIRECTION_SELL : (balance > 0 ? OrderDirection.ORDER_DIRECTION_SELL : OrderDirection.ORDER_DIRECTION_BUY);
            Long quantity = strategy.getDirection().equals("hold") ? Math.abs(balance) : strategy.getQuantity();
            //Выставляем заявку на покупку по лимитной цене
//            var orderId = api.getOrdersService()
//                    .postOrderSync(
//                            strategy.getFigi(),
//                            quantity,
//                            price,
//                            direction,
//                            mainAccountId,
//                            OrderType.ORDER_TYPE_MARKET,
//                            UUID.randomUUID().toString()).getOrderId();

//            getOrders(api, mainAccountId);
//            getPosition(api, mainAccountId);
//            getPortfolio(api, mainAccountId);
        }

        Message message = new Message();
        message.setSenderName("server");
        message.setMessage(userCache.getStrategies());
        message.setStatus(Status.JOIN);
        streamService.sendDataToUser(strategy.getUserName(), message);
    }

    private static List<OrderState> getOrders(InvestApi api, String accountId) {
        var orders = api.getOrdersService().getOrdersSync(accountId);
        for (OrderState order : orders) {
            log.info(order);
        }

        return orders;
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
        List<Account> listAccount = api.getUserService().getAccountsSync();
        PortfolioResponse portfolioResponse = getPortfolio(api, listAccount.get(0).getId());
        PositionsResponse positionsResponse = getPosition(api, listAccount.get(0).getId());
        if (listAccount.size() == 0) {
            //Открываем счёт
            String accountId = api.getSandboxService().openAccountSync();
            //Пополнить счёт
            api.getSandboxService().payIn(accountId, MoneyValue.newBuilder().setUnits(100000).setCurrency("RUB").build());
            List<Account> accounts = api.getUserService().getAccountsSync();
            return List.of(new AccountDto(accounts.get(0).getId(), portfolioResponse.getTotalAmountCurrencies().getUnits()));
        }
//        GetMarginAttributesResponse marginAttributesResponse=api.getUserService().getMarginAttributesSync(listAccount.get(0).getId());
        AccountDto accountDto = new AccountDto(listAccount.get(0).getId(), portfolioResponse.getTotalAmountCurrencies().getUnits());
        accountDto.setLogs(userCache.getLogs());
//        accountDto.setLastPrice(USER_STORE.get("Test").getMap().get("RIH3"));
//        accountDto.setLastTimeUpdate(USER_STORE.get("Test").getUpdateTime() != null ? new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date(USER_STORE.get("Test").getUpdateTime().getSeconds() * 1000)) : null);
        accountDto.setNotifications(COMMON_INFO.get("Notifications"));

//        if (positionsResponse.getSecuritiesCount() != 0) {
//            PositionsSecurities positionsSecurities = positionsResponse.getSecurities(0);
//            accountDto.setFigi(positionsSecurities.getFigi());
//            accountDto.setBalance(positionsSecurities.getBalance());
//            balance = positionsSecurities.getBalance();
//        } else {
//            balance = 0L;
//        }
        return List.of(accountDto);
    }
}
