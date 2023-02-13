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

    public void sendDataToUser(String userName, Message message) {
        simpMessagingTemplate.convertAndSendToUser(userName, "/private", message);
    }
}
