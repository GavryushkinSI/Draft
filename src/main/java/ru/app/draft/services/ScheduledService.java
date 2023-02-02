package ru.app.draft.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.TreeSet;

import static ru.app.draft.store.Store.CANDLE_STORE;

@Service
@Log4j2
public class ScheduledService {

//    @Scheduled(fixedDelay = 2000)
//    public void checkStreamMarketData() {
//        Date currentDate = new Date();
//        log.info(currentDate);
//        CANDLE_STORE.get("FUTRTS032300");
//    }
}