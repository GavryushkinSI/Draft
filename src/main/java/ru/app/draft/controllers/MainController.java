package ru.app.draft.controllers;

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
import ru.app.draft.services.ApiService;
import ru.app.draft.services.DbService;
import ru.app.draft.services.MarketDataStreamService;
import ru.app.draft.services.TelegramBotService;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.MarketDataSubscriptionService;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static ru.app.draft.store.Store.*;

@Log4j2
@RestController
public class MainController {

    private final MarketDataStreamService marketDataStreamService;
    private final TelegramBotService telegramBotService;
    private final DbService dbService;
    private final ApiService apiService;
    private final InvestApi api;

    public MainController(MarketDataStreamService marketDataStreamService, TelegramBotService telegramBotService, DbService dbService, ApiService apiService, InvestApi api) {
        this.marketDataStreamService = marketDataStreamService;
        this.telegramBotService = telegramBotService;
        this.dbService = dbService;
        this.apiService = apiService;
        this.api = api;
    }

    @Audit
    @MessageMapping("/message")
    public void registrationUserOnContent(@Payload Message message) {
        log.info("test");
    }

    @Audit
    @GetMapping("/app/getUserInfo/{userName}")
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
    public ResponseEntity<List<Ticker>> getAllTickers() {
        return ResponseEntity.ok(TICKERS.get("tickers"));
    }

    @Audit
    @GetMapping("/app/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("SUCCESS");
    }

    @GetMapping("/app/reconnect")
    @Audit
    public void reconnectStream() {
        Map<String, MarketDataSubscriptionService> subscriptionServiceMap = api.getMarketDataStreamService().getAllStreams();
        List<String> reconnectStreamList = new ArrayList<>();
        LAST_PRICE.forEach((k, v) -> {
            if (!subscriptionServiceMap.containsKey(k)) {
                reconnectStreamList.add(k);
            }
        });
        apiService.setSubscriptionOnCandle(api, reconnectStreamList);
    }

    @PostMapping("/app/tv")
    @Audit
    public void tradingViewSignalPoint(@RequestBody Strategy strategy) {
        apiService.sendOrder(api, strategy);
    }

    @Audit
    @PostMapping("/app/editStrategy/{userName}")
    public ResponseEntity<Collection<Strategy>> addOrUpdateStrategy(@PathVariable String userName, @RequestBody Strategy strategy) {
        UserCache userCache = USER_STORE.get(userName);
        List<Strategy> strategyList = userCache.getStrategies();
        if (StringUtils.hasText(strategy.getId())) {
            strategyList.set(Integer.parseInt(strategy.getId()), strategy);
        } else {
            List<String> figiList = apiService.getFigi(api, List.of(strategy.getTicker()));
            if (!LAST_PRICE.containsKey(figiList.get(0))) {
                apiService.setSubscriptionOnCandle(api, figiList);
                LAST_PRICE.put(figiList.get(0), new LastPrice(null, null));
            }
            String uuid = String.valueOf(strategyList.size());
            strategyList.add(new Strategy(uuid,
                    strategy.getUserName(),
                    strategy.getName(),
                    strategy.getDirection(),
                    strategy.getQuantity(),
                    figiList.get(0),
                    strategy.getTicker(),
                    strategy.getIsActive(),
                    strategy.getConsumer(),
                    new ArrayList<>()));
        }

        userCache.setStrategies(strategyList);
        USER_STORE.replace(userName, userCache);

        return ResponseEntity.ok(strategyList);
    }

    @PostMapping("/app/login/{status}")
    public ResponseEntity<String> registration(@RequestBody User user, @PathVariable String status) throws Exception {
        if (status.equals("enter")) {
            if (!USER_STORE.containsKey(user.getLogin())) {
                throw new Exception("Пользователя с таким именем нет...");
            }
            String hashPassword = USER_STORE.get(user.getLogin()).getUser().getPassword();
            return ResponseEntity.ok(hashPassword);
        }
        if (status.equals("reg")) {
            if (USER_STORE.containsKey(user.getLogin())) {
                throw new Exception("Логин занят...");
            }
            USER_STORE.put(user.getLogin(), new UserCache(user));
        }
        return null;
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
    public ResponseEntity<Collection<Strategy>> getAllStrategyByUser(@PathVariable String userName, HttpServletRequest request) {
//        if(request.getHeader("Authorization")==null){
//            throw new AuthorizationException();
//        }
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

//    @Audit
//    @Scheduled(fixedDelay = 500000)
//    public void getAllTickersTask() {
//        List<UserCache> userCaches =new ArrayList<>();
////        User user =new User("1", "2", "vvr", "rvbrbr");
////        UserCache userCache=new UserCache(user);
////        userCache.setStrategies(List.of(new Strategy(
////                "0","1","test", "buy", 0L, "tbt", "btb", true,
////                null, null
////        )));
////        userCaches.add(userCache);
//        dbService.deleteAll();
////        try {
////            Thread.sleep(3000);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//        dbService.saveUsers(userCaches);
//        //dbService.saveUsers(userCaches);
////        dbService.getAllUsers();
//    }


//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<?> handleException(Exception e) {
//            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
//    }
}


