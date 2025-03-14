package ru.app.draft;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.app.draft.utils.CommonUtils;

import java.util.TimeZone;

@SpringBootApplication
public class DraftApplication {
    public static void main(String[] args) {
        // Устанавливаем временную зону
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"));
        SpringApplication.run(DraftApplication.class, args);
        CommonUtils.clearLogFile();
    }
}
