package ru.app.draft.controllers;

import com.google.protobuf.Timestamp;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import ru.app.draft.annotations.Audit;
import ru.app.draft.models.*;
import ru.app.draft.services.ApiService;
import ru.app.draft.services.MarketDataStreamService;
import ru.app.draft.store.Store;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.MarketDataSubscriptionService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.app.draft.store.Store.USER_STORE;

@Log4j2
@RestController
public class MainController {

    private final MarketDataStreamService marketDataStreamService;
    private final ApiService apiService;
    private final InvestApi api;

    public MainController(MarketDataStreamService marketDataStreamService, ApiService apiService, InvestApi api) {
        this.marketDataStreamService = marketDataStreamService;
        this.apiService = apiService;
        this.api = api;
    }

    @Audit
    @MessageMapping("/message")
    public void registrationUserOnContent(@Payload Message message) {
        Store.changeUserInfo(message, marketDataStreamService);
    }

    @Audit
    @GetMapping("/app/getUserInfo/{userName}")
    public ResponseEntity<List<AccountDto>> getUserInfo(@PathVariable String userName) {
        return ResponseEntity.ok(apiService.getAccountInfo(api, userName));
    }

    @Audit
    @GetMapping("/app/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("SUCCESS");
    }

    @GetMapping("/app/reconnect")
    public void reconnectStream(){
        List<String> tickers = apiService.getFigi(api, List.of("RIH3"));
        MarketDataSubscriptionService service=api.getMarketDataStreamService().getStreamById("last_price_stream");
        if(service!=null) {
                service.unsubscribeLastPrices(tickers);
        }
        apiService.setSubscriptionOnCandle(api, tickers);
    }

    @PostMapping("/app/tv")
    public void tradingViewSignalPoint(@RequestBody Strategy strategy) {
        List<String> figi = apiService.getFigi(api, List.of(strategy.getTicker()));
        strategy.setFigi(figi.get(0));
        apiService.sendOrder(api, strategy);
    }

    @PostMapping("/app/editStrategy/{userName}")
    public ResponseEntity<Collection<Strategy>> addOrUpdateStrategy(@PathVariable String userName, @RequestBody Strategy strategy) {
        UserCache userCache = USER_STORE.get(userName);
        List<Strategy> strategyList = userCache.getStrategies();
        if (StringUtils.hasText(strategy.getId())) {
            strategyList.set(Integer.parseInt(strategy.getId()), strategy);
        } else {
            String uuid = String.valueOf(strategyList.size());
            strategyList.add(new Strategy(uuid,
                    strategy.getUserName(),
                    strategy.getName(),
                    strategy.getDirection(),
                    strategy.getQuantity(),
                    strategy.getFigi(),
                    strategy.getTicker(),
                    strategy.getIsActive(),
                    strategy.getConsumer(),
                    new ArrayList<>()));
        }

        userCache.setStrategies(strategyList);
        USER_STORE.replace(userName, userCache);

        return ResponseEntity.ok(strategyList);
    }

    @PostMapping("/app/login")
    public void registration(@RequestBody User user) throws Exception {
        USER_STORE.put(user.getLogin(), new UserCache(user));
//        if(!USER_STORE.containsKey(user.getLogin())){
//            throw new Exception("AUTH_FAIL");
//        }
    }

    @PostMapping("/app/deleteStrategy/{userName}/{name}")
    public ResponseEntity<Collection<Strategy>> removeStrategy(@PathVariable String userName, @PathVariable String name) {
        UserCache userCache = USER_STORE.get(userName);
        List<Strategy> strategyList = userCache.getStrategies();
        strategyList.removeIf(strategy -> strategy.getName().equals(name));
        userCache.setStrategies(strategyList);
        USER_STORE.replace(userName, userCache);
        return ResponseEntity.ok(strategyList);
    }

    @GetMapping("/app/clear/{userName}")
    public void clear(@PathVariable String userName) {
        USER_STORE.replace(userName, USER_STORE.get(userName).clearLog());
    }

    @GetMapping("/app/getAllStrategy/{userName}")
    public ResponseEntity<Collection<Strategy>> getAllStrategyByUser(@PathVariable String userName) {
        if (USER_STORE.containsKey(userName)) {
            return ResponseEntity.ok(USER_STORE.get(userName).getStrategies());
        }
        return ResponseEntity.ok(new ArrayList<>(0));
    }

    @Audit
    @Scheduled(fixedDelay = 3000)
    public void checkStreamMarketData() {
//        Timestamp time = USER_STORE.get("Test").getUpdateTime();
        List<String> tickers = apiService.getFigi(api, List.of("RIH3"));
//        if (time != null) {
//            if (Math.abs(time.getSeconds() - Instant.now().atZone(ZoneId.of("Europe/Moscow")).toEpochSecond()) > 300) {
//                log.error("Reconnect stream!");
//                api.getMarketDataStreamService().getStreamById("last_price_stream").unsubscribeLastPrices(tickers);
//                apiService.setSubscriptionOnCandle(api, tickers);
//            }
//        }
    }
}
