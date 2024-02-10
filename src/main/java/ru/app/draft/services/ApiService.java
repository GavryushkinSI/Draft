package ru.app.draft.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.app.draft.annotations.Audit;
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

import static ru.app.draft.store.Store.*;

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

    public Ticker getFigi(InvestApi api, List<String> tickers) {
        return TICKERS_TKS.get("tickers").stream().filter(i -> i.getValue().equals(tickers.get(0))).findFirst().get();
    }

    public void getAllTickers(InvestApi api, List<String> filter) {
            List<Ticker> tickers = api.getInstrumentsService().getAllFuturesSync().stream().map(i -> new Ticker(i.getTicker(), i.getTicker(), i.getFigi(), i.getClassCode(), BigDecimal.valueOf(i.getLot()))).collect(Collectors.toList());
        tickers.addAll(api.getInstrumentsService()
                .getAllSharesSync()
                .stream()
                .filter(i -> i.getClassCode().equals("TQBR"))
                .map(i -> new Ticker(i.getTicker(), i.getTicker(), i.getFigi(), i.getClassCode(), BigDecimal.valueOf(i.getLot())))
                .collect(Collectors.toList()));
        tickers = tickers.stream().filter(i -> filter.contains(i.getValue())).collect(Collectors.toList());
        TICKERS_TKS.replace("tickers", tickers);
        //ByBit
        List<Ticker> byBitTickers = List.of(
                new Ticker("BTCUSDT","BTCUSDT","BTCUSDT","BYBITFUT", BigDecimal.valueOf(0.001)),
                new Ticker("ETHUSDT","ETHUSDT","ETHUSDT","BYBITFUT", BigDecimal.valueOf(0.1)),
                new Ticker("APTUSDT","APTUSDT","APTUSDT","BYBITFUT", BigDecimal.valueOf(0.1)),
                new Ticker("ORDIUSDT","ORDIUSDT","ORDIUSDT","BYBITFUT", BigDecimal.valueOf(0.1)),
                new Ticker("FETUSDT","FETUSDT","FETUSDT","BYBITFUT", BigDecimal.valueOf(1.0)),
                new Ticker("XRPUSDT","XRPUSDT","XRPUSDT","BYBITFUT", BigDecimal.valueOf(1.0)),
                new Ticker("AVAXUSDT","XRPUSDT","XRPUSDT","BYBITFUT", BigDecimal.valueOf(0.1)),
                new Ticker("GRTUSDT","GRTUSDT","GRTUSDT","BYBITFUT", BigDecimal.valueOf(1.0))
        );
        TICKERS_BYBIT.replace("tickers", byBitTickers);
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
                List<String> figsList = new ArrayList<>();
                TICKERS_TKS.get("tickers").forEach(i -> figsList.add(i.getFigi()));
                this.setSubscriptionOnCandle(api, figsList);
            }
        }).subscribeLastPrices(figs);
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

    public void sendOrderDb(InvestApi api, Strategy strategy) {

    }

    public void sendOrder(InvestApi api, Strategy strategy) {
        //Выставляем заявку
        UserCache userCache = USER_STORE.get(strategy.getUserName());
        List<Strategy> strategyList = userCache.getStrategies();
        Strategy changingStrategy = strategyList
                .stream()
                .filter(str -> str.getName().equals(strategy.getName())).findFirst().get();
        if (!changingStrategy.getIsActive()) {
            return;
        }
        BigDecimal position = BigDecimal.ZERO;
        BigDecimal executionPrice = null;
        String time = null;
        ErrorData errorData = changingStrategy.getErrorData();
        long commissions=0L;
        OrderDirection direction = null;

        if (changingStrategy.getConsumer().contains("terminal")) {
            if (strategy.getDirection().equals("buy")) {
                direction = OrderDirection.ORDER_DIRECTION_BUY;
            } else if (strategy.getDirection().equals("sell")) {
                direction = OrderDirection.ORDER_DIRECTION_SELL;
            } else {
                if (changingStrategy.getCurrentPosition().longValue()<0) {
                    direction = OrderDirection.ORDER_DIRECTION_BUY;
                } else {
                    direction = OrderDirection.ORDER_DIRECTION_SELL;
                }
            }

            var accounts = api.getUserService().getAccountsSync();
            if (accounts.size() == 0) {
                //Открываем счёт
                String accountId = api.getSandboxService().openAccountSync();
                //Пополнить счёт
                api.getSandboxService().payIn(accountId, MoneyValue.newBuilder().setUnits(5000000).setCurrency("RUB").build());
                accounts = api.getUserService().getAccountsSync();
            }
            var mainAccount = accounts.get(0).getId();
            var price = Quotation.newBuilder().build();
            position = strategy.getQuantity().subtract(changingStrategy.getCurrentPosition());
//            //Выставляем заявку на покупку по рыночной цене
            PostOrderResponse orderResponse = null;
            time = DateUtils.getCurrentTime();
            try {
                orderResponse = api.getOrdersService()
                        .postOrderSync(
                                changingStrategy.getFigi(),
                                Math.abs(position.longValue()),
                                price,
                                direction,
                                mainAccount,
                                OrderType.ORDER_TYPE_MARKET,
                                UUID.randomUUID().toString());
            } catch (Exception e) {
                errorData.setMessage(e.getMessage());
                errorData.setTime(time);
                return;
            }
            executionPrice = CommonUtils.getFromMoneyValue(orderResponse.getExecutedOrderPrice());
            commissions = orderResponse.getExecutedCommission().getUnits();
            if (useSandbox) {
                Optional<PortfolioPosition> portfolioPosition = getPortfolio(api, mainAccount).getPositionsList().stream().filter(i -> i.getFigi().equals(changingStrategy.getFigi())).findFirst();

                if (portfolioPosition.isPresent()) {
                    var realQuantity = portfolioPosition.get().getQuantity();
                    if (realQuantity.getUnits() != strategy.getQuantity().longValue()) {
                        errorData.setMessage(String.format("Фактическое вол-во лотов:%s, планируемое:%s", realQuantity.getUnits(), strategy.getQuantity()));
                        errorData.setTime(time);
                        changingStrategy.setErrorData(errorData);
                        return;
                    }
                } else {
                    if (strategy.getQuantity().longValue() != 0) {
                        errorData.setMessage(String.format("Фактическое вол-во лотов:%s, планируемое:%s", 0, strategy.getQuantity()));
                        errorData.setTime(time);
                        changingStrategy.setErrorData(errorData);
                        return;
                    }
                }
                if (!CollectionUtils.isEmpty(getOrders(api, mainAccount))) {
                    errorData.setMessage("Есть неисполненные ордера!");
                    errorData.setTime(time);
                    return;
                }
            }
        } else if (changingStrategy.getConsumer().contains("test")) {
            executionPrice = LAST_PRICE.get(changingStrategy.getFigi()).getPrice();
            position = strategy.getQuantity().subtract(changingStrategy.getCurrentPosition());
        }
        userCache.addCommission(commissions);
        updateStrategyCache(strategyList, strategy, changingStrategy, executionPrice, userCache, position, time);
        Message message = new Message();
        message.setSenderName("server");
        message.setMessage(userCache.getStrategies());
        message.setCommand("strategy");
        message.setStatus(Status.JOIN);
        streamService.sendDataToUser(Set.of(strategy.getUserName()), message);
    }

    private void updateStrategyCache(List<Strategy> strategyList, Strategy strategy, Strategy changingStrategy, BigDecimal executionPrice, UserCache userCache, BigDecimal position, String time) {
        var minLot = changingStrategy.getMinLot();
        String printPrice = CommonUtils.formatNumber(executionPrice);

        if (strategy.getDirection().equals(changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) > 0 ? "buy" : changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0 ? "sell" : "hold")) {
            changingStrategy.addEnterAveragePrice(executionPrice, false);
        } else {
            changingStrategy.addEnterAveragePrice(executionPrice, true);
        }

        if (strategy.getDirection().equals("buy")) {
            changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition().add(position));
            for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
                changingStrategy.addOrder(new Order(executionPrice, minLot, strategy.getDirection(), time));
            }
            String text = String.format("%s => Покупка %s лотов по цене %s (priceTV:%s). Время %s.", strategy.getName(), Math.abs(position.doubleValue()), printPrice, strategy.getPriceTv(), time);
            userCache.addLogs(text);
            if (userCache.getUser().getChatId() != null && changingStrategy.getConsumer().contains("telegram")) {
                telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
            }
        }
        if (strategy.getDirection().equals("sell")) {
            changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition().add(position));
            for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
                changingStrategy.addOrder(new Order(executionPrice, minLot, strategy.getDirection(), time));
            }
            String text = String.format("%s => Продажа %s лотов по цене %s (priceTV:%s). Время %s.", strategy.getName(), Math.abs(position.doubleValue()), printPrice, strategy.getPriceTv(), time);
            userCache.addLogs(text);
            if (userCache.getUser().getChatId() != null && changingStrategy.getConsumer().contains("telegram")) {
                telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
            }
        }
        if (strategy.getDirection().equals("hold")) {
            if (changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) != 0) {
                String text = String.format(changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0 ? "%s => Покупка %s лотов по цене %s (priceTV:%s). Время %s." : "%s => Продажа %s лотов по цене %s (priceTV:%s). Время %s.", strategy.getName(), Math.abs(position.doubleValue()), printPrice, strategy.getPriceTv(), time);
                userCache.addLogs(text);
                if (userCache.getUser().getChatId() != null && changingStrategy.getConsumer().contains("telegram")) {
                    telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
                }
                for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
                    changingStrategy.addOrder(new Order(executionPrice, minLot, changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0 ? "buy" : "sell", time));
                }
                changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition().add(position));
            }
        }

        strategyList.set(Integer.parseInt(changingStrategy.getId()), changingStrategy);
        userCache.setStrategies(strategyList);
        USER_STORE.replace(strategy.getUserName(), userCache);
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
