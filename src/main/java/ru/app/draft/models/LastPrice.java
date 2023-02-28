package ru.app.draft.models;

import com.google.protobuf.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static ru.app.draft.store.Store.LAST_PRICE;

@Getter
@Setter
public class LastPrice {
    Long price;
    List<String> nameSubscriber = new ArrayList<>();
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
