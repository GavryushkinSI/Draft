package ru.app.draft.services;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.app.draft.models.LastPrice;
import ru.app.draft.models.Strategy;
import ru.app.draft.models.Ticker;
import ru.app.draft.models.UserCache;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ru.app.draft.store.Store.LAST_PRICE;
import static ru.app.draft.store.Store.USER_STORE;

@Service
public class CommonStrategyServiceImpl {

    private final ApiService apiService;
    private final ByBitService byBitService;
    private final InvestApi api;

    public CommonStrategyServiceImpl(ApiService apiService, ByBitService byBitService, InvestApi api) {
        this.apiService = apiService;
        this.byBitService = byBitService;
        this.api = api;
    }

    public void addOrUpdateStrategy(String userName, Strategy strategy){
        UserCache userCache = USER_STORE.get(userName);
        List<Strategy> strategyList = userCache.getStrategies();
        if (StringUtils.hasText(strategy.getId())) {
            Strategy changingStrategy = strategyList.get(Integer.parseInt(strategy.getId()));
            strategy.setFigi(strategy.getTicker());
            strategy.setMinLot(changingStrategy.getMinLot());
            strategy.setOrders(changingStrategy.getOrders());
            strategy.setEnterAveragePrice(changingStrategy.getEnterAveragePrice());
            strategyList.set(Integer.parseInt(strategy.getId()), strategy);
        } else {
            Ticker ticker;
            if (!Objects.equals(strategy.getProducer(), "BYBIT")) {
                ticker = apiService.getFigi(api, List.of(strategy.getTicker()));
            }else{
                ticker = byBitService.getFigi(List.of(strategy.getTicker()));
            }
                if (!LAST_PRICE.containsKey(ticker.getFigi())) {
                    LAST_PRICE.put(ticker.getFigi(), new LastPrice(null, null));
                }
                LastPrice lastPrice = LAST_PRICE.get(ticker.getFigi());
                lastPrice.addSubscriber(userName);
                LAST_PRICE.replace(ticker.getFigi(), lastPrice);
            strategyList.add(new Strategy(
                    String.valueOf(strategyList.size()),
                    strategy.getUserName(),
                    strategy.getName(),
                    strategy.getDirection(),
                    strategy.getQuantity(),
                    ticker.getFigi(),
                    strategy.getTicker(),
                    strategy.getIsActive(),
                    strategy.getConsumer(),
                    new ArrayList<>(),
                    strategy.getDescription(),
                    ticker.getMinLot(),
                    strategy.getProducer()));
        }

        userCache.setStrategies(strategyList);
        USER_STORE.replace(userName, userCache);
    }
}
