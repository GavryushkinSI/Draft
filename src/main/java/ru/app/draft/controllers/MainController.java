package ru.app.draft.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.app.draft.annotations.Audit;
import ru.app.draft.models.Message;
import ru.app.draft.services.MarketDataStreamService;
import ru.app.draft.store.Store;
import ru.tinkoff.piapi.core.InvestApi;

@RestController
public class MainController {

    private final MarketDataStreamService marketDataStreamService;

    public MainController(MarketDataStreamService marketDataStreamService) {
        this.marketDataStreamService = marketDataStreamService;
    }

    @Audit
    @MessageMapping("/message")
    public void registrationUserOnContent(@Payload Message message) {
        Store.changeUserInfo(message, marketDataStreamService);
    }
}
