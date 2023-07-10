package ru.app.draft.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.app.draft.models.MetricItem;
import ru.app.draft.models.Notification;

import java.util.List;

import static ru.app.draft.store.Store.COMMON_INFO;
import static ru.app.draft.store.Store.METRICS;

@RestController
public class AddController {

    @GetMapping("/app/getAllArticles")
    public List<Notification> getAllArticles() {
        return COMMON_INFO.get("Notifications");
    }
}
