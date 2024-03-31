package ru.app.draft.config;

import com.bybit.api.client.config.BybitApiConfig;
import com.bybit.api.client.restApi.*;
import com.bybit.api.client.service.BybitApiClientFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.app.draft.models.Notification;
import ru.app.draft.services.ApiService;
import ru.app.draft.services.ByBitService;
import ru.app.draft.services.DbService;
import ru.app.draft.services.TelegramBotService;
import ru.tinkoff.piapi.core.InvestApi;

import java.sql.Date;
import java.time.Instant;
import java.util.*;

import static ru.app.draft.store.Store.*;

@Log4j2
@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final List<String> myTickers = List.of(
            "EuH3", "GKH3", "SRH3", "RIH3", "BRH3", "SiH3", "EDH3", "ALH3", "POH3", "MNH3", "AFH3", "VBH3", "SFH3", "NAH3", "MMH3", "CRH3",
            "EuM3", "GKM3", "SRM3", "RIM3", "BRM3", "SiM3", "EDM3", "ALM3", "POM3", "MNM3", "AFM3", "VBM3", "SFM3", "NAM3", "MMM3", "CRM3",
            "EuU3", "GKU3", "SRU3", "RIU3", "BRU3", "SiU3", "EDU3", "ALU3", "POU3", "MNU3", "AFU3", "VBU3", "SFU3", "NAU3", "MMU3", "CRU3",
            "EuZ3", "GKZ3", "SRZ3", "RIZ3", "BRZ3", "SiZ3", "EDZ3", "ALZ3", "POZ3", "AFZ3", "VBZ3", "SFZ3", "NAZ3", "MMZ3", "CRZ3","GZZ3", "MNZ3", "LKZ3","GDZ3","SVZ3",
            "BRN3",
            "SBER", "GAZP", "MGNT", "MOEX", "AFKS", "BANE", "HYDR", "PLZL", "VTBR", "ROSN", "MTSS", "GMKN");

    @Bean
    public ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
//        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        return mapper;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("http://localhost", "*", "http://89.223.68.98", "http://89.223.68.98:80")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/chatroom", "/user");
        registry.setUserDestinationPrefix("/user");
    }

    @Bean
    public InvestApi createApi(@Value("${sandbox.token:123}") String sandboxToken, @Value("${real.token:123}") String realToken, @Value("${usesandbox}") Boolean useSandbox, @Qualifier("apiService") ApiService apiService, DbService dbService, ByBitService byBitService) {
        //InvestApi api = InvestApi.createSandbox(sandboxToken);
        InvestApi api = useSandbox ? InvestApi.createSandbox(sandboxToken) : InvestApi.create(realToken);
        COMMON_INFO.put("Notifications", new ArrayList<>());
        COMMON_INFO.computeIfPresent("Notifications", (s, data) -> {
            data.add(new Notification("О проекте... Релиз v.01.000.00.",
                    "<p><strong><span ><span style=\"text-shadow: rgba(136, 136, 136, 0.8) 2px 3px 2px;\">TView_Parser</span>&nbsp;</span></strong><span >- это молодой проект и на текущий момент ещё не весь функционал доступен. Но если ты интересуешься автоматизацией биржевой торговли, то уже сейчас ты можешь протестировать свои идеи без риска для своего капитала. Для этого тебе понадобится аккаунт на TradingView и возможности данного ресурса.<br></span></p>\n" +
                            "<p><strong><span >Подробности релиза v.01.000.00:</span></strong></p>\n" +
                            "<p><span ><strong><span style=\"color: rgb(97, 189, 109);\">1.</span>&nbsp;</strong>Реализована возможность приёма сигнала по WebHook от TradingView. На текущий момент все заведённые стратегии работают в режиме эмуляции, т.е. без выставления ордеров на биржу. В рамках данного режима можно посмотреть перспективность выбранной стратегии без риска для капитала. По каждой стратегии выводится график доходности. Все стратегии работают на текущий момент только с рыночными ордерами, что предполагает 100% исполнение всех ордеров.&nbsp;</span></p>\n" +
                            "<p><span ><strong><span style=\"color: rgb(184, 49, 47);\">Работа с тейк-профит и стоп-лосс.</span></strong></span></p>\n" +
                            "<p><span >Классические стоп ордера не используется (речь идём о том, что когда пользователь выставляет стоп-лосс, фактически он отправляет стоп-ордер на сервер брокера, где в дальнейшем уже в случае достижения ценой уровня стоп-лосса &nbsp;происходит отправка заявки на биржу).</span></p>\n" +
                            "<p><span >Стоп-лоссы и тейк-профиты, &nbsp;в том числе с трейлингом без проблем можно реализовать средствами pinescript и добавить в свою стратегию. TView_Parser будет отрабатывать эти ордера, как рыночные.</span></p>\n" +
                            "<p><span ><strong><span style=\"color: rgb(65, 168, 95);\">2.</span></strong> Реализована возможность подключения уведомлений о сигналах в телеграмм (нужно подписаться на tview_bot).</span></p>\n" +
                            "<p><span ><strong><span style=\"color: rgb(97, 189, 109);\">3.</span>&nbsp;</strong>Реализован блок пользовательский уведомлений.&nbsp;</span></p>\n" +
                            "<p><span >&nbsp;Сюда буду приходить ссылки на важные новости (описание последнего релиза, полезные статьи) + критические ошибки.&nbsp;Уведомление о сделках реализовано в виде всплывающих нотификаций.</span></p>\n",
                    "info_success",
                    "modal",
                    false,
                    true, Date.from(Instant.ofEpochMilli(1672531200)).toString()));

            data.add(new Notification("Автоматический бактестинг стратегии в TradingView с сохранением результатов в CSV",
                    "<p>Если вы используете стратегии в трейдингвью, например чтобы быстро накидать прототип идеи из какого нибудь источника и посмотреть её, то у вас наверняка также появлялся вопрос поиска приемлемых параметров и проверка как они влияют на стратегию. Делать это вручную крайне трудозатратно. Простейшая стратегия двух скользящих средних может давать 400 и более вариантов параметров. А любое увеличение кол-ва параметров и диапазона их значений приводит к необходимости перебора значений растущих в геометрической прогрессии. Например стратегия из 5 параметров по 15 значений дает 15 ^ 5 = 759 375 вариантов. Подобрать их руками, когда один вариант вычисляется пару секунд нереально.</p>\n" +
                            "<p>А можно ли автоматизировать этот процесс? Ниже описание решения через расширение для браузера на основе Chrome.</p>\n" +
                            "<p>Подробности по ссылке: <a data-fr-linked=\"true\" href=\"https://smart-lab.ru/blog/724466.php\">https://smart-lab.ru/blog/724466.php</a>.</p>",
                    "info_success",
                    "modal",
                    false,
                    true,
                    java.util.Date.from(Instant.ofEpochMilli(1740960000)).toString()));

            data.add(new Notification("Бесплатный Premium доступ на Tradingview",
                    "<p><strong>100% надёжного </strong>способа получения бесплатного доступа к аккаунту TradingView<strong><span style=\"color: rgb(184, 49, 47);\">&nbsp;не существует.</span></strong></p>\n" +
                            "<p>Причина простая. TradingView постоянно совершенствует свои методы поиска таких клиентов-халявщиков.</p>\n" +
                            "<p>Тем не менее у меня есть положительный опыт использования предложений представленных на&nbsp;</p>\n" +
                            "<p><a data-fr-linked=\"true\" href=\"https://plati.market/search/tradingview\">https://plati.market/search/tradingview</a>. <strong>Стоимость там небольшая, как правило,&nbsp; 200-300руб.</strong> &nbsp;Поэтому даже если подписка всё таки следит, в целом не так и обидно.</p>\n" +
                            "<p>В любом случае продавцы на данном ресурсе крайне отзывчива и при необходимости наверняка помогут вам понять, что стало причиной обнуления вашей подписки (обычно это кривой vpn или просто забывчивость его вовремя включить).</p>\n" +
                            "<p>Если ли же вы не хотите заморачиваться с vpn, прокси и другими обходными система, то рекомендую купить аккаунт TradingView с оплатой криптовалютой. Сейчас можно оплатить месяц (раньше к слову сказать криптовалютой можно было оплатить только годовую подписку).</p>\n" +
                            "<p><strong>Инcтрукция по оплате: </strong><a data-fr-linked=\"true\" href=\"https://trading-shop.ru/instruktsii/kak-oplatit-tradingview-iz-rossii/\">https://trading-shop.ru/instruktsii/kak-oplatit-tradingview-iz-rossii/</a></p>\n" +
                            "<p>Всё работает, проверено мною лично.</p>\n" +
                            "<p>Единственный на мой взгляд минус - это минимальный объем вывода с биржи <a href=\"https://www.pexpay.com/ru\" rel=\"noopener noreferrer\" target=\"_blank\">PexPay</a>. Вывести сумму менее 50 USDT просто невозможно. Соответственно купить подписку на месяц меньше, &nbsp;чем premium у вас не получиться. &nbsp;Но в целом эту проблему можно решить использование других бирж, либо кошельков, где порог вывода ниже. Если есть надёжные варианты прошу указывать их в комментариях ниже.</p>",
                    "info_success",
                    "modal",
                    false,
                    true,
                    java.util.Date.from(Instant.ofEpochMilli(1740960001)).toString()));
            return data;
        });
        METRICS.put("methods", new ArrayList<>());
        TICKERS_TKS.put("tickers", new ArrayList<>());
        TICKERS_BYBIT.put("tickers", new ArrayList<>());
        apiService.getAllTickers(api, myTickers);
        dbService.getAllUsers();

        List<String> figsList = new ArrayList<>();
        TICKERS_TKS.get("tickers").forEach(i -> figsList.add(i.getFigi()));
//        if(!useSandbox){
//            apiService.setPortfolioStream(api);
//            apiService.setOrdersStream(api);
//        }
        //apiService.setSubscriptionOnCandle(api, figsList);
        dbService.getAllComments();
        byBitService.setStreamPublic();
        byBitService.setStreamPrivate();
        return api;
    }

    @Bean
    public TelegramBotsApi initTelegramBot(TelegramBotService service) {
        TelegramBotsApi telegramBotsApi = null;
        try {
            telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(service);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }

        return telegramBotsApi;
    }

    @Bean
    public BybitApiTradeRestClient createByBitRestClientOrder(@Value("${bybit.key}") String key,@Value("${bybit.serial}") String serial){
        return BybitApiClientFactory.newInstance(key, serial, BybitApiConfig.MAINNET_DOMAIN).newTradeRestClient();
    }

    @Bean
    public BybitApiPositionRestClient createByBitRestPositionClient(@Value("${bybit.key}") String key,@Value("${bybit.serial}") String serial){
       return BybitApiClientFactory.newInstance(key, serial, BybitApiConfig.MAINNET_DOMAIN).newPositionRestClient();
    }

    @Bean
    public BybitApiMarketRestClient createMarketDataRestClient(){
        return BybitApiClientFactory.newInstance(BybitApiConfig.MAINNET_DOMAIN,true).newMarketDataRestClient();
    }

    @Bean
    public BybitApiLendingRestClient createLendingRestClient(@Value("${bybit.key}") String key,@Value("${bybit.serial}") String serial)
    {
        return BybitApiClientFactory.newInstance(key, serial).newLendingRestClient();
    }

    @Bean
    public BybitApiAccountRestClient getAccountInfo(@Value("${bybit.key}") String key,@Value("${bybit.serial}") String serial){
        return BybitApiClientFactory.newInstance(key, serial, BybitApiConfig.MAINNET_DOMAIN).newAccountRestClient();
    }

    @Bean
    public BybitApiAsyncTradeRestClient createAsyncClient(@Value("${bybit.key}") String key,@Value("${bybit.serial}") String serial){
        return BybitApiClientFactory.newInstance(key, serial, BybitApiConfig.MAINNET_DOMAIN).newAsyncTradeRestClient();
    }
}