package ru.app.draft.models;

import com.google.protobuf.Timestamp;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@Getter
@Setter
@ToString
public class UserCache {
    private Map<String, Long> map = new HashMap<>();
    private List<Strategy> strategies = new ArrayList<>();
    private List<String> logs = new ArrayList<>();
    private Timestamp updateTime;
    private User user;

    public UserCache(User user) {
        this.user = user;
    }

    public void addLogs(String log) {
        logs.add(log);
    }

    public UserCache clearLog() {
        logs.clear();
        return this;
    }
}
