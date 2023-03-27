package ru.app.draft.config;

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
import ru.app.draft.models.Strategy;
import ru.app.draft.models.UserCache;
import ru.app.draft.services.ApiService;
import ru.app.draft.services.DbService;
import ru.app.draft.services.TelegramBotService;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.app.draft.store.Store.*;

@Log4j2
@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final List<String> myTickers = List.of(
            "EuH3", "GKH3", "SRH3", "RIH3", "BRH3", "SiH3","EDH3","ALH3","POH3","MNH3","AFH3",
            "VBH3", "SFH3", "NAH3", "MMH3", "CRH3",
            "EuM3", "GKM3", "SRM3", "RIM3", "BRM3", "SiM3","EDM3","ALM3","POM3","MNM3","AFM3",
            "VBM3", "SFM3", "NAM3", "MMM3", "CRM3",
            "SBER", "GAZP","MGNT","MOEX","AFKS","BANE", "HYDR", "PLZL", "VTBR","ROSN", "MTSS", "GMKN");

    @Bean
    public ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
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
    public InvestApi createApi(@Value("${token:123}") String token, @Qualifier("apiService") ApiService apiService, DbService dbService) {
        InvestApi api = InvestApi.createSandbox(token);
        COMMON_INFO.put("Notifications", new ArrayList<>());
        COMMON_INFO.computeIfPresent("Notifications", (s, data) -> {
            data.add(new Notification("О проекте...",
                    "TView_parser - это молодой проект и на текущий момент ещё не весь функционал доступен. " +
                            "Но если ты интересуешься автоматизацией биржевой торговли, то уже сейчас ты можешь протестировать свои идеи без риска для своего капитала." +
                            " Для этого тебе понадобится аккаунт на TradingView и возможности нашего ресурса.",
                    "info_success",
                    "modal",
                    false,
                    false));
            return data;
        });
        TICKERS.put("tickers", new ArrayList<>());
        apiService.getAllTickers(api, myTickers);
        dbService.getAllUsers();

        List<String> figsList=new ArrayList<>();
        TICKERS.get("tickers").forEach(i->figsList.add(i.getFigi()));
        apiService.setSubscriptionOnCandle(api, figsList);

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
}