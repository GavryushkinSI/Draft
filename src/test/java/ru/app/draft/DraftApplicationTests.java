//package ru.app.draft;
//
//import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
//import com.bybit.api.client.restApi.BybitApiTradeRestClient;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.junit.runner.RunWith;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.context.junit4.SpringRunner;
//import org.springframework.test.web.servlet.MockMvc;
//import ru.app.draft.models.LastPrice;
//import ru.app.draft.models.Strategy;
//import ru.app.draft.models.Ticker;
//import ru.app.draft.models.UserCache;
//import ru.app.draft.repository.StrategyRepository;
//import ru.app.draft.services.ByBitService;
//import ru.app.draft.services.CommonStrategyServiceImpl;
//import ru.app.draft.services.MarketDataStreamService;
//import ru.app.draft.services.TelegramBotService;
//
//import java.math.BigDecimal;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static ru.app.draft.store.Store.*;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
//@EnableAutoConfiguration
//@RunWith(SpringRunner.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class DraftApplicationTests {
//
//    private Strategy strategy;
//    private Strategy strategyTv;
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @Autowired
//    ObjectMapper mapper;
//
//    @Autowired
//    ByBitService byBitService;
//
//    @Autowired
//    StrategyRepository strategyRepository;
//
//    @Autowired
//    CommonStrategyServiceImpl commonStrategyService;
//
//    @MockBean
//    BybitApiTradeRestClient orderRestClient;
//    @MockBean
//    TelegramBotService telegramBotService;
//    @MockBean
//    MarketDataStreamService streamService;
//
//    private final static String USER = "Admin";
//
//    private static Map<String, Object> map = new LinkedHashMap<>();
//
//    private static UserCache userCache = null;
//
//    void fillTickers() {
//        List<Ticker> byBitTickers = List.of(
//                new Ticker("BTCUSDT", "BTCUSDT", "BTCUSDT", "BYBITFUT", BigDecimal.valueOf(0.001)),
//                new Ticker("ETHUSDT", "ETHUSDT", "ETHUSDT", "BYBITFUT", BigDecimal.valueOf(0.1))
//        );
//        TICKERS_BYBIT.replace("tickers", byBitTickers);
//    }
//
//    @BeforeAll
//    void setup() {
//        fillTickers();
//        userCache = USER_STORE.get(USER);
//        userCache.getStrategies().clear();
//        strategy = new Strategy();
//        strategy.setName("test");
//        strategy.setIsActive(Boolean.TRUE);
//        strategy.setTicker("BTCUSDT");
//        strategy.setProducer("BYBIT");
//        strategy.setUserName("Admin");
//        strategy.setConsumer(List.of("terminal"));
//
//        strategyRepository.deleteAll();
//        commonStrategyService.addOrUpdateStrategy(USER, strategy);
//
//        List<Strategy> strategyList = userCache.getStrategies();
//        strategy.setId(strategyList.get(0).getId());
//        assertEquals(strategy.getName(), strategyList.get(0).getName());
//
//        map.put("retCode", 0);
//        Mockito.when(orderRestClient.createOrder(any(TradeOrderRequest.class))).thenReturn(map);
//        Mockito.when(orderRestClient.amendOrder(any(TradeOrderRequest.class))).thenReturn(map);
//        Mockito.doNothing().when(telegramBotService).sendMessage(0L, "");
//        Mockito.doNothing().when(streamService).sendDataToUser(Set.of(), null);
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(1000), null));
//    }
//    @AfterAll
//    void tearDown(){
//        strategy.setCurrentPosition(BigDecimal.ZERO);
//        strategy.setErrorData(null);
//        commonStrategyService.addOrUpdateStrategy(USER, strategy);
//    }
//
//    @Test
//    void positiveTerminal() throws Exception {
//        assertEquals(0,userCache.getStrategies().get(0).getCurrentPosition().intValue());
//        //покупка (указываем какая позиция должны быть)
//        buy(null, null, null);
//        assertEquals(BigDecimal.valueOf(0.001), userCache.getStrategies().get(0).getCurrentPosition());
//        //продажа с переворотом
//        sell(-0.001, null, null);
//        assertEquals(BigDecimal.valueOf(-0.001), userCache.getStrategies().get(0).getCurrentPosition());
//        //выход из позиции
//        buy(0.0, null, null);
//        assertEquals(0, userCache.getStrategies().get(0).getCurrentPosition().intValue());
//        //Последовательная покупка
//        buy(0.001, null, null);
//        buy(0.001, null, null);
//        assertEquals(BigDecimal.valueOf(0.001), userCache.getStrategies().get(0).getCurrentPosition());
//        buy(0.002, null, null);
//        assertEquals(BigDecimal.valueOf(0.002), userCache.getStrategies().get(0).getCurrentPosition());
//        //продажа половины
//        sell(0.001, null, null);
//        sell(0.001, null, null);
//        assertEquals(BigDecimal.valueOf(0.001), userCache.getStrategies().get(0).getCurrentPosition());
//        sell(-0.001, null, null); //продажа с переворотом
//        assertEquals(BigDecimal.valueOf(-0.001), userCache.getStrategies().get(0).getCurrentPosition());
//        assertNull(userCache.getStrategies().get(0).getErrorData().getMessage());
//    }
//
//    @Test
//    void negative() {
//        strategy.setConsumer(List.of("terminal"));
//        UserCache userCache = USER_STORE.get(USER);
//        map.clear();
//        map.put("retCode", 10);
//        buy(null, null, null);
//        assertNotNull(userCache.getStrategies().get(0).getErrorData().getMessage());
//        strategy.setErrorData(null);
//        commonStrategyService.addOrUpdateStrategy(USER, strategy);
//    }
//
//    @Test
//    void testConsumer() {
//        strategy.setConsumer(List.of("test"));
//        commonStrategyService.addOrUpdateStrategy(USER, strategy);
//        //покупка (указываем какая позиция должны быть)
//        buy(null, null, null);
//        assertEquals(BigDecimal.valueOf(0.001), userCache.getStrategies().get(0).getCurrentPosition());
//        //продажа с переворотом
//        sell(-0.001, null, null);
//        sell(-0.001, null, null);
//        assertEquals(BigDecimal.valueOf(-0.001), userCache.getStrategies().get(0).getCurrentPosition());
//        //выход из позиции
//        buy(0.0, null, null);
//        assertEquals(0, userCache.getStrategies().get(0).getCurrentPosition().intValue());
//        assertNull(userCache.getStrategies().get(0).getErrorData().getMessage());
//    }
//
//    private void buy(Double quantity, String orderName, Double triggerPrice) {
//        strategyTv = new Strategy();
//        strategyTv.setName("test");
//        strategyTv.setProducer("BYBIT");
//        strategyTv.setUserName("Admin");
//        if(orderName!=null){
//            strategyTv.setDirection("buy");
//            strategyTv.setOrderName(orderName);
//            strategyTv.setQuantity(BigDecimal.valueOf(Objects.isNull(quantity) ? 0.001 : quantity));
//            strategyTv.setTriggerPrice(BigDecimal.valueOf(triggerPrice));
//        }else{
//            strategyTv.setDirection("buy");
//            strategyTv.setQuantity(BigDecimal.valueOf(Objects.isNull(quantity) ? 0.001 : quantity));
//        }
//        byBitService.sendSignal(strategyTv);
//    }
//
//    private void sell(Double quantity, String orderName, Double triggerPrice) {
//        strategyTv = new Strategy();
//        strategyTv.setName("test");
//        strategyTv.setProducer("BYBIT");
//        strategyTv.setUserName("Admin");
//        if(orderName!=null){
//            strategyTv.setDirection("sell");
//            strategyTv.setOrderName(orderName);
//            strategyTv.setQuantity(BigDecimal.valueOf(Objects.isNull(quantity) ? 0.001 : quantity));
//            strategyTv.setTriggerPrice(BigDecimal.valueOf(triggerPrice));
//        }else{
//            strategyTv.setDirection("sell");
//            strategyTv.setQuantity(BigDecimal.valueOf(Objects.isNull(quantity) ? -0.001 : quantity));
//        }
//        byBitService.sendSignal(strategyTv);
//    }
//
//    @Test
//    void conditionalOrdersTestConsumer(){
//        strategy.setConsumer(List.of("test"));
//        commonStrategyService.addOrUpdateStrategy(USER, strategy);
//        checkConditionalOrders();
//    }
//
//    @Test
//    void conditionalOrdersTerminal(){
//        strategy.setConsumer(List.of("terminal"));
//        commonStrategyService.addOrUpdateStrategy(USER, strategy);
//        checkConditionalOrders();
//    }
//
//    private void checkLastPrice(){
//        byBitService.setCurrentPosition(null, null, null, LAST_PRICE.get("BTCUSDT").getPrice());
//    }
//
//    private void checkExecOrder(BigDecimal expectedPrice){
//        Strategy checkStrategy = userCache.getStrategies().get(0);
//        assertEquals(expectedPrice, checkStrategy.getOrders().get(checkStrategy.getOrders().size()-1).getPrice());
//    }
//
//    private void checkConditionalOrders(){
//        assertEquals(0,userCache.getStrategies().get(0).getCurrentPosition().intValue());
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(0), null));
//        buy(0.003d, "entry_buy", 10000d);
//        assertEquals(0,userCache.getStrategies().get(0).getCurrentPosition().intValue());
//        sell(-0.003d, "entry_sell", 9000d);
//        assertEquals(ORDERS_MAP.size(),2);
//        assertEquals(0,userCache.getStrategies().get(0).getCurrentPosition().intValue());
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10100), null));
//        checkLastPrice();
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10300), null));
//        checkLastPrice();
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10310), null));
//        buy(0.003d, "entry_buy", 10000d);
//        checkLastPrice();
//        checkExecOrder(BigDecimal.valueOf(10100));
//        assertEquals(BigDecimal.valueOf(0.003),userCache.getStrategies().get(0).getCurrentPosition());
//        sell(0.001d, "part_close", 10200d);
//        checkLastPrice();
//        sell(0.001d, "part_close", 10210d);
//        checkLastPrice();
//        assertEquals(ORDERS_MAP.size(),3);
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10200), null));
//        checkLastPrice();
//        checkExecOrder(BigDecimal.valueOf(10200));
//        assertEquals(BigDecimal.valueOf(0.001),userCache.getStrategies().get(0).getCurrentPosition());
//        sell(-0.003d, "entry_sell", 10000d);
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10100), null));
//        sell(-0.003d, "entry_sell", 10000d);
//        checkLastPrice();
//        assertEquals(BigDecimal.valueOf(0.001),userCache.getStrategies().get(0).getCurrentPosition());
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(9900), null));
//        checkLastPrice();
//        buy(0.003d, "entry_buy", 10300d);
//        checkLastPrice();
//        assertEquals(BigDecimal.valueOf(-0.003),userCache.getStrategies().get(0).getCurrentPosition());
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10010), null));
//        checkLastPrice();
//        buy(0.003d, "entry_buy", 10500d);
//        assertEquals(BigDecimal.valueOf(-0.003),userCache.getStrategies().get(0).getCurrentPosition());
//        buy(-0.001d, "part_close", 10050d);
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10010), null));
//        checkLastPrice();
//        assertEquals(BigDecimal.valueOf(-0.003),userCache.getStrategies().get(0).getCurrentPosition());
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10020), null));
//        checkLastPrice();
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10060), null));
//        checkLastPrice();
//        checkExecOrder(BigDecimal.valueOf(10060));
//        assertEquals(BigDecimal.valueOf(-0.001),userCache.getStrategies().get(0).getCurrentPosition());
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10200), null));
//        checkLastPrice();
//        assertEquals(BigDecimal.valueOf(-0.001),userCache.getStrategies().get(0).getCurrentPosition());
//        buy(0.003d, "entry_buy", 10500d);
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(10510), null));
//        checkLastPrice();
//        sell(0.0d, "part_close", 10000d);
//        assertEquals(BigDecimal.valueOf(0.003),userCache.getStrategies().get(0).getCurrentPosition());
//        LAST_PRICE.replace("BTCUSDT", new LastPrice(BigDecimal.valueOf(9000), null));
//        checkLastPrice();
//        checkExecOrder(BigDecimal.valueOf(9000));
//        assertEquals(0,userCache.getStrategies().get(0).getCurrentPosition().intValue());
//        assertNull(userCache.getStrategies().get(0).getErrorData().getMessage());
//        ORDERS_MAP.clear();
//
//
////        String x= "{\"userName\"+:+\"Admin\",\"name\"+:+\"strategy\",\"direction\"+:+\"{{strategy.order.action}}\",\"quantity\"+:+\"{{strategy.position.size}}\",\"producer\"+:+\"BYBIT\"}";
////        String y = "\"orderName\"+:+\"entry_buy\",\"triggerPrice\"+:+\"{{order.price}}\"";
//    }
//}
