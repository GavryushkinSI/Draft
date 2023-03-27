package ru.app.draft.models;

import com.google.protobuf.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ru.app.draft.store.Store.LAST_PRICE;

@Getter
@Setter
public class LastPrice {
    Long price;
    Set<String> nameSubscriber = new HashSet<>();
    Timestamp updateTime;

    public LastPrice(Long price, Timestamp updateTime) {
        this.price = price;
        this.updateTime = updateTime;
    }

    public void addSubscriber(String name) {
        this.nameSubscriber.add(name);
    }

    public void removeSubscriber(String name) {
            this.nameSubscriber.remove(name);
    }
}
