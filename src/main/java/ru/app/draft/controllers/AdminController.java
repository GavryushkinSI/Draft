package ru.app.draft.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.app.draft.annotations.Audit;
import ru.app.draft.models.Notification;
import ru.app.draft.models.ShortLastPrice;
import ru.app.draft.models.User;
import ru.app.draft.services.DbService;
import ru.app.draft.services.MarketDataStreamService;
import ru.app.draft.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static ru.app.draft.store.Store.*;

@Log4j2
@RestController
public class AdminController {

    private final MarketDataStreamService marketDataStreamService;

    public AdminController(MarketDataStreamService marketDataStreamService) {
        this.marketDataStreamService = marketDataStreamService;
    }

    @Audit
    @GetMapping("/app/adminTickers")
    public ResponseEntity<List<ShortLastPrice>> getInfoTickers() {
        List<ShortLastPrice> list = new ArrayList<>();
        LAST_PRICE.forEach((k, v) -> {
            if (v.getUpdateTime() != null) {
                list.add(new ShortLastPrice(k, v.getPrice(), DateUtils.getTime(v.getUpdateTime().getSeconds())));
            }
        });

        return ResponseEntity.ok(list);
    }

    @Audit
    @PostMapping("/app/addArticle")
    public void addArticle(@RequestBody Notification notification) {
        COMMON_INFO.computeIfPresent("Notifications", (s, notifications) -> {
            notifications.add(new Notification(notification.getHeader(),
                    notification.getMessage(),
                    "info_success",
                    "modal",
                    false,
                    notification.getBlockCommentEnabled()));
            return notifications;
        });
    }

    public List<User> getAllUsers(){
        List<User> users = new ArrayList<>();
        USER_STORE.values().forEach(user-> {
            User us =new User();
            us.setLogin(user.getUser().getLogin());
            us.setLastVisit(user.getUser().getLastVisit());
            users.add(us);
        });

        return users;
    }
}
