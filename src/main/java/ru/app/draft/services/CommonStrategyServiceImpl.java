package ru.app.draft.services;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.app.draft.models.LastPrice;
import ru.app.draft.models.StrategyTv;
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

    public void addOrUpdateStrategy(String userName, StrategyTv strategyTv){
        UserCache userCache = USER_STORE.get(userName);
        List<StrategyTv> strategyTvList = userCache.getStrategies();
        if (StringUtils.hasText(strategyTv.getId())) {
            StrategyTv changingStrategyTv = strategyTvList.get(Integer.parseInt(strategyTv.getId()));
            strategyTv.setFigi(strategyTv.getTicker());
            strategyTv.setMinLot(changingStrategyTv.getMinLot());
            strategyTv.setOrders(changingStrategyTv.getOrders());
            strategyTv.setEnterAveragePrice(changingStrategyTv.getEnterAveragePrice());
            strategyTvList.set(Integer.parseInt(strategyTv.getId()), strategyTv);
        } else {
            Ticker ticker;
            if (!Objects.equals(strategyTv.getProducer(), "BYBIT")) {
                ticker = apiService.getFigi(List.of(strategyTv.getTicker()));
            }else{
                ticker = byBitService.getFigi(List.of(strategyTv.getTicker()));
            }
                if (!LAST_PRICE.containsKey(ticker.getFigi())) {
                    LAST_PRICE.put(ticker.getFigi(), new LastPrice(null, null));
                }
                LastPrice lastPrice = LAST_PRICE.get(ticker.getFigi());
                lastPrice.addSubscriber(userName);
                LAST_PRICE.replace(ticker.getFigi(), lastPrice);
            strategyTvList.add(new StrategyTv(
                    String.valueOf(strategyTvList.size()),
                    strategyTv.getUserName(),
                    strategyTv.getName(),
                    strategyTv.getDirection(),
                    strategyTv.getQuantity(),
                    ticker.getFigi(),
                    strategyTv.getTicker(),
                    strategyTv.getIsActive(),
                    strategyTv.getConsumer(),
                    new ArrayList<>(),
                    strategyTv.getDescription(),
                    ticker.getMinLot(),
                    strategyTv.getProducer()));
        }

        userCache.setStrategies(strategyTvList);
        USER_STORE.replace(userName, userCache);
    }
}
