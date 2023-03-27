package ru.app.draft.services;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ru.app.draft.models.Message;
import ru.app.draft.models.UserCache;

import java.util.List;
import java.util.Map;
import java.util.Set;


@Service
public class MarketDataStreamService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public MarketDataStreamService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void sendDataToUser(Set<String> userNameList, Message message) {
        userNameList.forEach(i->simpMessagingTemplate.convertAndSendToUser(i, "/private", message));
    }
}
