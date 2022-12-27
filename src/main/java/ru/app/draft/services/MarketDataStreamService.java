package ru.app.draft.services;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.app.draft.models.Message;
import ru.app.draft.models.UserCache;

import java.util.Map;


@Service
public class MarketDataStreamService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public MarketDataStreamService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void sendDataToUser(Map<String, UserCache> activeUsers, Message message){
        activeUsers.forEach((k, v) -> {
                simpMessagingTemplate.convertAndSendToUser(v.getUserName(), "/private", message);
        });
    }
}
