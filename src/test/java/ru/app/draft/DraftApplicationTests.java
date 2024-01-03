package ru.app.draft;

import com.bybit.api.client.domain.trade.request.TradeOrderRequest;
import com.bybit.api.client.restApi.BybitApiTradeRestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ru.app.draft.models.Strategy;
import ru.app.draft.models.Ticker;
import ru.app.draft.models.UserCache;
import ru.app.draft.repository.StrategyRepository;
import ru.app.draft.services.ByBitService;
import ru.app.draft.services.CommonStrategyServiceImpl;
import ru.app.draft.services.MarketDataStreamService;
import ru.app.draft.services.TelegramBotService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static ru.app.draft.store.Store.TICKERS_BYBIT;
import static ru.app.draft.store.Store.USER_STORE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableAutoConfiguration
@RunWith(SpringRunner.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DraftApplicationTests {

    private Strategy strategy;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    ByBitService byBitService;

    @Autowired
    StrategyRepository strategyRepository;

    @Autowired
    CommonStrategyServiceImpl commonStrategyService;

    @MockBean
    BybitApiTradeRestClient orderRestClient;
    @MockBean
    TelegramBotService telegramBotService;
    @MockBean
    MarketDataStreamService streamService;

    private final static String USER = "Admin";

    private static Map<String, Object> map = new LinkedHashMap<>();

    private static UserCache userCache = null;

    void fillTickers() {
        List<Ticker> byBitTickers = List.of(
                new Ticker("BTCUSDT", "BTCUSDT", "BTCUSDT", "BYBITFUT", BigDecimal.valueOf(0.001)),
                new Ticker("ETHUSDT", "ETHUSDT", "ETHUSDT", "BYBITFUT", BigDecimal.valueOf(0.1))
        );
        TICKERS_BYBIT.replace("tickers", byBitTickers);
    }

    @BeforeAll
    void setup() {
        fillTickers();
        userCache = USER_STORE.get(USER);
        userCache.getStrategies().clear();
        strategy = new Strategy();
        strategy.setName("test");
        strategy.setIsActive(Boolean.TRUE);
        strategy.setTicker("BTCUSDT");
        strategy.setProducer("BYBIT");
        strategy.setUserName("Admin");
        strategy.setConsumer(List.of("terminal"));
        strategyRepository.deleteAll();
        commonStrategyService.addOrUpdateStrategy(USER, strategy);

        List<Strategy> strategyList = userCache.getStrategies();
        strategy.setId(strategyList.get(0).getId());
        assertEquals(strategy.getName(), strategyList.get(0).getName());

        map.put("retCode", 0);
        Mockito.when(orderRestClient.createOrder(any(TradeOrderRequest.class))).thenReturn(map);
        map.put("price", "20000");
        map.put("qty", "0.001");
        Mockito.when(orderRestClient.getHistoryOrderResult(any(TradeOrderRequest.class))).thenReturn(map);
        Mockito.doNothing().when(telegramBotService).sendMessage(0L, "");
        Mockito.doNothing().when(streamService).sendDataToUser(Set.of(), null);
    }


    @Test
    void positive() throws Exception {
        UserCache userCache = USER_STORE.get(USER);
        //покупка (указываем какая позиция должны быть)
        buy(null);
        byBitService.sendSignal(strategy);
        assertEquals(BigDecimal.valueOf(0.001), userCache.getStrategies().get(0).getCurrentPosition());
        //продажа с переворотом
        sell(-0.001);
        byBitService.sendSignal(strategy);
        assertEquals(BigDecimal.valueOf(-0.001), userCache.getStrategies().get(0).getCurrentPosition());
        //выход из позиции
        buy(0.0);
        byBitService.sendSignal(strategy);
        assertEquals(0, userCache.getStrategies().get(0).getCurrentPosition().intValue());
        //Последовательная покупка
        buy(0.001);
        byBitService.sendSignal(strategy);
        assertEquals(BigDecimal.valueOf(0.001), userCache.getStrategies().get(0).getCurrentPosition());
        buy(0.002);
        byBitService.sendSignal(strategy);
        assertEquals(BigDecimal.valueOf(0.002), userCache.getStrategies().get(0).getCurrentPosition());
        //продажа половины
        sell(0.001);
        byBitService.sendSignal(strategy);
        assertEquals(BigDecimal.valueOf(0.001), userCache.getStrategies().get(0).getCurrentPosition());
        sell(-0.001); //продажа с переворотом
        byBitService.sendSignal(strategy);
        assertEquals(BigDecimal.valueOf(-0.001), userCache.getStrategies().get(0).getCurrentPosition());
        assertNull(userCache.getStrategies().get(0).getErrorData().getMessage());
    }

    @Test
    void negative() {
        UserCache userCache = USER_STORE.get(USER);
        map.clear();
        map.put("retCode", 10);
        buy(null);
        byBitService.sendSignal(strategy);
        assertNotNull(userCache.getStrategies().get(0).getErrorData().getMessage());
    }

    @Test
    void testConsumer() {
        strategy.setConsumer(List.of("test"));
        commonStrategyService.addOrUpdateStrategy(USER, strategy);
        //покупка (указываем какая позиция должны быть)
        buy(null);
        byBitService.sendSignal(strategy);
        assertEquals(BigDecimal.valueOf(0.001), userCache.getStrategies().get(0).getCurrentPosition());
        //продажа с переворотом
        sell(-0.001);
        byBitService.sendSignal(strategy);
        assertEquals(BigDecimal.valueOf(-0.001), userCache.getStrategies().get(0).getCurrentPosition());
        //выход из позиции
        buy(0.0);
        byBitService.sendSignal(strategy);
        assertEquals(0, userCache.getStrategies().get(0).getCurrentPosition().intValue());
    }

    private void buy(Double quantity) {
        strategy.setDirection("buy");
        strategy.setQuantity(BigDecimal.valueOf(Objects.isNull(quantity) ? 0.001 : quantity));
    }

    private void sell(Double quantity) {
        strategy.setDirection("sell");
        strategy.setQuantity(BigDecimal.valueOf(Objects.isNull(quantity) ? -0.001 : quantity));
    }
}
