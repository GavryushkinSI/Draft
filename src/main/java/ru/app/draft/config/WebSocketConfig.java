package ru.app.draft.config;

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
import ru.app.draft.models.LastPrice;
import ru.app.draft.models.Notification;
import ru.app.draft.models.UserCache;
import ru.app.draft.services.ApiService;
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
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("http://localhost", "*", "http://89.223.68.98", "http://89.223.68.98:80").withSockJS();
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
}