package ru.app.draft.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import ru.app.draft.models.CandleData;
import ru.app.draft.models.Message;
import ru.app.draft.models.Status;
import ru.app.draft.models.UserCache;
import ru.app.draft.store.Store;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Log4j2
@Component
public class ApiService {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final MarketDataStreamService streamService;

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

    public void setSubscriptionOnCandle(InvestApi api, List<String> figs) {
        StreamProcessor<MarketDataResponse> processor = (response) -> {
            if (response.hasTradingStatus()) {
                log.info("Новые данные по статусам: {}", response);
            } else if (response.hasPing()) {
                log.info("пинг сообщение");
            } else if (response.hasCandle()) {
                Candle candle = response.getCandle();
                String date = dateFormat.format(new Date(candle.getTime().getSeconds() * 1000));
                CandleData candleData = new CandleData(date, List.of(candle.getOpen().getUnits(), candle.getHigh().getUnits(), candle.getLow().getUnits(), candle.getClose().getUnits()));
                Store.CANDLE_STORE.computeIfPresent(candle.getFigi(), (s, data) -> {
                    data.add(candleData);
                    Message message = new Message();
                    message.setSenderName("server");
                    message.setMessage(candleData);
                    message.setStatus(Status.JOIN);
                    Map<String, UserCache> map = Store.USER_STORE.entrySet().stream().filter(stringUserCacheEntry -> Objects.equals(stringUserCacheEntry.getValue().getTicker(),candle.getFigi())).collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
                    streamService.sendDataToUser(map, message);
                    return data;
                });
            } else if (response.hasTrade()) {
                Trade trade = response.getTrade();
                log.info("Новые данные по сделкам: {}", response);
            } else if (response.hasLastPrice()) {
                log.info("Последняя цена: {}", response);
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
                log.info("удачных подписок на последние цены: {}", successCount);
                log.info("неудачных подписок на последние цены: {}", errorCount);
            }
        };

        api.getMarketDataStreamService().newStream("candles_stream", processor, log::error).subscribeCandles(figs);
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
}
