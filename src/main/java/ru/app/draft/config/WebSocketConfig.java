package ru.app.draft.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotOptions;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import org.telegram.telegrambots.meta.generics.TelegramBot;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.app.draft.models.LastPrice;
import ru.app.draft.models.Notification;
import ru.app.draft.models.UserCache;
import ru.app.draft.services.ApiService;
import ru.app.draft.services.DbService;
import ru.app.draft.services.TelegramBotService;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static ru.app.draft.store.Store.*;

@Log4j2
@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebSocketConfig implements  WebSocketMessageBrokerConfigurer {


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
    public InvestApi createApi(@Value("${token}") String token, @Qualifier("apiService") ApiService apiService) {
        InvestApi api = InvestApi.createSandbox(token);
        COMMON_INFO.put("Notifications", new ArrayList<>());
        COMMON_INFO.computeIfPresent("Notifications", (s, data) -> {
            data.add(new Notification("Привет!..", "info", null));
            return data;
        });
        TICKERS.put("tickers", new ArrayList<>());
        apiService.getAllTickers(api);
        return api;
    }

    @Bean
    public TelegramBotsApi initTelegramBot(TelegramBotService service, DbService dbService) {
        dbService.getAllUsers();
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