package ru.app.draft.controllers;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
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
import ru.app.draft.utils.CommonUtils;
import ru.app.draft.utils.DateUtils;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.MarketDataSubscriptionService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static ru.app.draft.models.EventLog.*;
import static ru.app.draft.store.Store.*;

@Slf4j
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
    public void tradingViewSignalPoint(@RequestBody StrategyTv strategyTv) throws InterruptedException {
        log.info(String.format("[%s] Сигнал от TradingView => nameTs: %s, direction:%s, doLots:%s, comment: %s, triggerPrice:%s", SIGNAL_FROM_TV, strategyTv.getName(), strategyTv.getDirection(), strategyTv.getQuantity(), strategyTv.getComment(), strategyTv.getTriggerPrice()));
        if (Objects.equals(strategyTv.getProducer(), "BYBIT")) {
            byBitService.sendSignal(strategyTv);
        } else {
            apiService.sendSignal(api, strategyTv);
        }
    }

    @Audit
    @PostMapping("/app/editStrategy/{userName}")
    public ResponseEntity<Collection<StrategyTv>> addOrUpdateStrategy(@PathVariable String userName, @RequestBody StrategyTv strategyTv) {
        UserCache userCache = USER_STORE.get(userName);
        List<StrategyTv> strategyTvList = userCache.getStrategies();
        if (StringUtils.hasText(strategyTv.getId())) {
            StrategyTv changingStrategyTv = strategyTvList.get(Integer.parseInt(strategyTv.getId()));
            strategyTv.setFigi(strategyTv.getTicker());
            strategyTv.setMinLot(changingStrategyTv.getMinLot());
            strategyTv.setOrders(changingStrategyTv.getOrders());
            strategyTv.setEnterAveragePrice(changingStrategyTv.getEnterAveragePrice());
            strategyTvList.set(Integer.parseInt(strategyTv.getId()), strategyTv);
            userCache.setStrategies(strategyTvList);
            USER_STORE.replace(userName, userCache);
        } else {
            Ticker ticker;
            if (!Objects.equals(strategyTv.getProducer(), "BYBIT")) {
                ticker = apiService.getFigi(List.of(strategyTv.getTicker()));
            } else {
                ticker = byBitService.getFigi(List.of(strategyTv.getTicker()));
            }
            if (!LAST_PRICE.containsKey(ticker.getFigi())) {
                LAST_PRICE.put(ticker.getFigi(), new LastPrice(null, null));
            }
            LastPrice lastPrice = LAST_PRICE.get(ticker.getFigi());
            lastPrice.addSubscriber(userName);
            LAST_PRICE.replace(ticker.getFigi(), lastPrice);
            StrategyTv newStrategyTv = new StrategyTv(
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
                    strategyTv.getProducer());
            newStrategyTv.setCreatedDate(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            newStrategyTv.setPriceScale(ticker.getPriceScale());
            newStrategyTv.setOptions(strategyTv.getOptions());
            strategyTvList.add(newStrategyTv);
            userCache.setStrategies(strategyTvList);
            USER_STORE.replace(userName, userCache);
            if (!Objects.equals(strategyTv.getProducer(), "BYBIT")) {
                apiService.setSubscriptionOnCandle(api, List.of(ticker.getFigi()));
            } else {
                byBitService.getPosition(ticker.getValue(), true);
                byBitService.sendInPublicWebSocket(ticker.getFigi());
            }
        }

        return ResponseEntity.ok(strategyTvList);
    }

    @Audit
    @PostMapping("/app/login/{status}")
    public ResponseEntity<String> registration(@RequestBody User user, @PathVariable String status) {
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
    public ResponseEntity<Collection<StrategyTv>> removeStrategy(@PathVariable String userName, @PathVariable String name) {
        UserCache userCache = USER_STORE.get(userName);
        List<StrategyTv> strategyTvList = userCache.getStrategies();
        StrategyTv findStrategyTv = strategyTvList.stream().filter(strategy -> strategy.getName().equals(name)).findFirst().get();
        ORDERS_MAP.remove(findStrategyTv.getName());
        strategyTvList.remove(findStrategyTv);
        userCache.setStrategies(strategyTvList);
        USER_STORE.replace(userName, userCache);
        return ResponseEntity.ok(strategyTvList);
    }

    @GetMapping("/app/clear/{userName}")
    public void clear(@PathVariable String userName) {
        USER_STORE.replace(userName, USER_STORE.get(userName).clearLog());
        CommonUtils.clearLogFile();
    }

    @GetMapping("/app/getAllStrategy/{userName}")
    public ResponseEntity<Collection<StrategyTv>> getAllStrategyByUser(@PathVariable String userName, HttpServletRequest request) {
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

    @Scheduled(fixedDelay = 1000000)
    public void getPositionInfo() {
        log.debug("Вызов автопроцедуры...");
        byBitService.getPositionInfoByPeriod();
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

    @PostMapping("/app/cancelAllOrders/{ticker}")
    public void cancelAllOrders(@PathVariable String ticker) {
        byBitService.cancelOrders(ticker, false, null);
    }


    @GetMapping("/app/getLogs/{filter}")
    public String getLogs(@PathVariable String filter) {
        return CommonUtils.readLogFile(filter);
    }

    @GetMapping("app/getClosedPnl/{date}")
    public Map<String, Set<Pnl>> getClosedPnl(@PathVariable Long date) {
        try {
            return byBitService.getClosedPnl(date);
        } catch (Exception e) {
            log.info(String.format("[%s]=> message:%s", ERROR, e.getMessage() + e.getCause() + " TRACE: " + Arrays.toString(e.getStackTrace())));
            throw e;
        }
    }

    @PostMapping("/app/testTelegramChannel")
    public ResponseEntity<Map<String, BigDecimal>> testTelegramCryptoChannel(@RequestBody List<TelegramSignal> listOfSignals) throws IOException {
        SimpleDateFormat dF = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        int count = 0;
        for (TelegramSignal signal : listOfSignals) {
            try {
                var res = byBitService.getMarketDataForTelegramTest(signal);
                var time = dF.format(new Date(signal.getTime()));
                var timeClose = res.getTimeClose() != null ? dF.format(new Date(res.getTimeClose())) : null;
                var pnl=res.getPnl()!=null?Math.ceil(res.getPnl().doubleValue()):0;
//                if(res.getOpen()!=null) {
                map.put(count + ":" + signal.getSymbol() + "||:" + signal.getDirection() + "||open:" + res.getOpen() + "||time:" + time + "||timeClose:" + timeClose + "||quantity:" + res.getQuantity() + "||countAdd:" + res.getCountAdd() + "||countFix:" + res.getCountFix() + "||closePrice:" + res.getClosePrice() + "||avrOpenPrice:" + res.getAverageOpenPrice() + "||fixByStop:" + res.getFixByStop() + "||positionFix:" + res.getPositionFix() + "||pnl:" + pnl + "%", res.getProfit());
                count++;
//                }
                log.debug(signal.getSymbol() + "=>" + "success..." + time);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        BigDecimal totalSum = map.values()
                .stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LinkedHashMap<String, BigDecimal> newMap = new LinkedHashMap<>();
        newMap.put("TotalSum", totalSum);
        newMap.putAll(map);

        return ResponseEntity.ok(newMap);
    }
}