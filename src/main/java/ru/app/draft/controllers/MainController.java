package ru.app.draft.controllers;

import io.micrometer.core.annotation.Timed;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import ru.app.draft.annotations.Audit;
import ru.app.draft.exceptions.AuthorizationException;
import ru.app.draft.models.*;
import ru.app.draft.services.*;
import ru.app.draft.utils.DateUtils;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.MarketDataSubscriptionService;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.app.draft.store.Store.*;

@Log4j2
@RestController
public class MainController {

    private final MarketDataStreamService marketDataStreamService;
    private final TelegramBotService telegramBotService;
    private final DbService dbService;
    private final ApiService apiService;
    private final ByBitService byBitService;
    private final InvestApi api;

    public MainController(MarketDataStreamService marketDataStreamService, TelegramBotService telegramBotService, DbService dbService, ApiService apiService, ByBitService byBitService, InvestApi api) {
        this.marketDataStreamService = marketDataStreamService;
        this.telegramBotService = telegramBotService;
        this.dbService = dbService;
        this.apiService = apiService;
        this.byBitService = byBitService;
        this.api = api;
    }

    @Audit
    @MessageMapping("/message")
    public void registrationUserOnContent(@Payload Message messageFromUser) {
        UserCache userCache = USER_STORE.get(messageFromUser.getSenderName());
        User user = userCache.getUser();
        user.setLastVisit(DateUtils.getCurrentTime());
        userCache.setUser(user);
        USER_STORE.replace(messageFromUser.getSenderName(), userCache);

        LAST_PRICE.forEach((k, v) -> {
            if (v.getUpdateTime() != null) {
                Message message = new Message();
                message.setSenderName("server");
                message.setMessage(new ShortLastPrice(k, v.getPrice(), null, null, DateUtils.getTime(v.getUpdateTime().getSeconds())));
                message.setStatus(Status.JOIN);
                message.setCommand("lastPrice");
                marketDataStreamService.sendDataToUser(Set.of(messageFromUser.getSenderName()), message);
            }
        });
    }

    @Audit
    @GetMapping("/app/getUserInfo/{userName}")
    @SuppressWarnings("ALL")
    public ResponseEntity<List<AccountDto>> getUserInfo(@PathVariable String userName) {
        return ResponseEntity.ok(apiService.getAccountInfo(api, userName));
    }

    @PostMapping("/app/feedback/{name}")
    public void feedback(@PathVariable String name, @RequestBody String text) {
        telegramBotService.sendMessage(
                Long.parseLong(USER_STORE.get("Admin").getUser().getChatId()),
                String.format("Сообщение от пользователя %s:%s", name, text));
    }

    @Audit
    @GetMapping("/app/getAllTickers")
    public ResponseEntity<Map<String, List<Ticker>>> getAllTickers() {
        return ResponseEntity.ok(Map.of(
                "TKS", TICKERS_TKS.get("tickers"),
                "BYBIT", TICKERS_BYBIT.get("tickers")
        ));
    }

    @GetMapping("/app/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("SUCCESS");
    }

    @GetMapping("/app/reconnect")
    public void reconnectStream() {
        Map<String, MarketDataSubscriptionService> subscriptionServiceMap = api.getMarketDataStreamService().getAllStreams();
        List<String> reconnectStreamList = new ArrayList<>();
    }

    @Audit
    @PostMapping("/app/tv")
    @Timed(value = "myapp.method.execution_time", description = "Execution time of the method")
    public void tradingViewSignalPoint(@RequestBody Strategy strategy) {
        sendLogFromTv(strategy);
        if (Objects.equals(strategy.getProducer(), "BYBIT")) {
            byBitService.sendSignal(strategy);
        } else {
            apiService.sendOrder(api, strategy);
        }
    }

    @Audit
    @PostMapping("/app/editStrategy/{userName}")
    public ResponseEntity<Collection<Strategy>> addOrUpdateStrategy(@PathVariable String userName, @RequestBody Strategy strategy) {
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
            } else {
                ticker = byBitService.getFigi(List.of(strategy.getTicker()));
            }
            if (!LAST_PRICE.containsKey(ticker.getFigi())) {
                LAST_PRICE.put(ticker.getFigi(), new LastPrice(null, null));
            }
            LastPrice lastPrice = LAST_PRICE.get(ticker.getFigi());
            lastPrice.addSubscriber(userName);
            LAST_PRICE.replace(ticker.getFigi(), lastPrice);
            Strategy newStrategy = new Strategy(
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
                    strategy.getProducer());
            newStrategy.setOptions(strategy.getOptions());
            newStrategy.setCurrentPosition(byBitService.getPosition(newStrategy.getFigi()));
            strategyList.add(newStrategy);
        }

        userCache.setStrategies(strategyList);
        USER_STORE.replace(userName, userCache);

        return ResponseEntity.ok(strategyList);
    }

    @Audit
    @PostMapping("/app/login/{status}")
    public ResponseEntity<String> registration(@RequestBody User user, @PathVariable String status) throws Exception {
        if (status.equals("enter")) {
            if (!USER_STORE.containsKey(user.getLogin())) {
                throw new AuthorizationException(new ErrorData("Пользователя с таким именем нет..."));
            }
            String hashPassword = USER_STORE.get(user.getLogin()).getUser().getPassword();
            return ResponseEntity.ok(hashPassword);
        }
        if (status.equals("reg")) {
            if (USER_STORE.containsKey(user.getLogin())) {
                throw new AuthorizationException(new ErrorData("Данный логин занят!.."));
            }
            USER_STORE.put(user.getLogin(), new UserCache(user));
        }
        return null;
    }

    @PostMapping("/app/deleteStrategy/{userName}/{name}")
    public ResponseEntity<Collection<Strategy>> removeStrategy(@PathVariable String userName, @PathVariable String name) {
        UserCache userCache = USER_STORE.get(userName);
        List<Strategy> strategyList = userCache.getStrategies();
        Strategy findStrategy = strategyList.stream().filter(strategy -> strategy.getName().equals(name)).findFirst().get();
        strategyList.remove(findStrategy);
        userCache.setStrategies(strategyList);
        USER_STORE.replace(userName, userCache);
        return ResponseEntity.ok(strategyList);
    }

    @GetMapping("/app/clear/{userName}")
    public void clear(@PathVariable String userName) {
        USER_STORE.replace(userName, USER_STORE.get(userName).clearLog());
    }

    @GetMapping("/app/getAllStrategy/{userName}")
    public ResponseEntity<Collection<Strategy>> getAllStrategyByUser(@PathVariable String userName, HttpServletRequest request) {
        if (USER_STORE.containsKey(userName)) {
            return ResponseEntity.ok(USER_STORE.get(userName).getStrategies());
        }
        return ResponseEntity.ok(new ArrayList<>(0));
    }

    @GetMapping("/app/saveDataInTable/{userName}")
    public void saveDataInTable(@PathVariable String userName) {
        Optional<UserCache> userCache = USER_STORE.values().stream().filter(i -> i.getUser().getLogin().equals(userName)).findFirst();
        if (userCache.isPresent() && userCache.get().getUser().getIsAdmin()) {
            dbService.deleteAll();
            dbService.saveUsers(new ArrayList<>(USER_STORE.values()));
        } else {
            throw new RuntimeException();
        }
    }

    @GetMapping("/app/getCountStreams")
    public ResponseEntity<ArrayList<String>> getCountStreams() {
        Map<String, MarketDataSubscriptionService> subscriptionServiceMap = api.getMarketDataStreamService().getAllStreams();
        return ResponseEntity.ok(new ArrayList<>(subscriptionServiceMap.keySet()));
    }

    @Scheduled(fixedDelay = 100000)
    public void getAllTickersTask() {
        LAST_PRICE.forEach((k, v) -> {
            if (v.getUpdateTime() != null) {
                Message message = new Message();
                message.setSenderName("server");
                message.setMessage(new ShortLastPrice(k, v.getPrice(), null, null, DateUtils.getTime(v.getUpdateTime().getSeconds())));
                message.setStatus(Status.JOIN);
                message.setCommand("lastPrice");
                Set<String> subscriber = v.getNameSubscriber();
                marketDataStreamService.sendDataToUser(subscriber, message);
            }
        });
    }

    @GetMapping("/app/getQuotes")
    public void getQuotes() {
        getAllTickersTask();
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity handleException(AuthorizationException e) {
        return new ResponseEntity(e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/app/unsubscribe")
    public void unsubscribed() {
        Map<String, MarketDataSubscriptionService> streams = api.getMarketDataStreamService().getAllStreams();
        streams.forEach((k, v) -> v.unsubscribeLastPrices(List.of(k)));
    }

    @PutMapping("/app/changeNotify")
    public ResponseEntity<List<Notification>> changeModify(@RequestBody Notification notification) {
        COMMON_INFO.computeIfPresent("Notifications", (s, notifications) -> notifications.stream().map(i -> {
            if (i.getId().equals(notification.getId())) {
                i = notification;
            }
            return i;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(COMMON_INFO.get("Notifications"));
    }

    @PostMapping("/app/setViewedNotifyIds/{userName}")
    public ResponseEntity<Set<String>> setViewedNotifyIds(@RequestBody List<String> ids, @PathVariable String userName) {
        UserCache userCache = USER_STORE.values().stream().filter(i -> i.getUser().getLogin().equals(userName)).findFirst().get();
        User user = userCache.getUser();
        user.addViewedNotifyIds(ids);
        userCache.setUser(user);
        USER_STORE.replace(userName, userCache);
        return ResponseEntity.ok(user.getViewedNotifyIds());
    }

    @DeleteMapping("/app/cancelAllOrders")
    public void cancelAllOrders() {
        byBitService.cancelOrders("BTC");
    }

    private void sendLogFromTv(Strategy strategy) {
        Message message = new Message();
        message.setSenderName("server");
        message.setMessage(String.format("%s => %s, %s, comment: %s, time :%s", strategy.getName(), strategy.getDirection(), strategy.getQuantity(), strategy.getComment(), DateUtils.getCurrentTime()));
        message.setStatus(Status.JOIN);
        message.setCommand("log");
        marketDataStreamService.sendDataToUser(Set.of("Admin"), message);
    }

    @Scheduled(fixedDelay = 30000)
    public void checkCurrentPosition() {
        var size = byBitService.getPosition("BTCUSDT");
        var side = size.doubleValue() > 0 ? "buy" : size.doubleValue() < 0 ? "sell" : "None";
        byBitService.setCurrentPosition(null, null, null, null, side, null, BigDecimal.valueOf(Math.abs(size.doubleValue())), "BTCUSDT");
    }
}


