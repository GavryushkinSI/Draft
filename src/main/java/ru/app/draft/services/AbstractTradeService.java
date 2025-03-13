package ru.app.draft.services;

import ru.app.draft.models.*;
import ru.app.draft.utils.CommonUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import static ru.app.draft.store.Store.USER_STORE;

public abstract class AbstractTradeService {

    private final TelegramBotService telegramBotService;
    private final MarketDataStreamService streamService;

    protected AbstractTradeService(TelegramBotService telegramBotService, MarketDataStreamService streamService) {
        this.telegramBotService = telegramBotService;
        this.streamService = streamService;
    }


    public void sendSignal(StrategyTv strategyTv) throws InterruptedException {

    }

    abstract Object getPositionInfo();

    public synchronized void updateStrategyCache(List<StrategyTv> strategyTvList, StrategyTv strategyTv, StrategyTv changingStrategyTv, BigDecimal executionPrice, UserCache userCache, BigDecimal position, String time, Boolean ordersFromTv, String orderLinkId) {
        if (executionPrice != null) {
            var minLot = changingStrategyTv.getMinLot();
            String printPrice = CommonUtils.formatNumber(executionPrice, changingStrategyTv.getPriceScale());

            if (strategyTv.getDirection().equals("buy")) {
                changingStrategyTv.setCurrentPosition(changingStrategyTv.getCurrentPosition().add(position));
//                for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
//                    changingStrategyTv.addOrder(new Order(executionPrice, minLot, strategyTv.getDirection(), time, orderLinkId));
//                }
                String text = String.format("%s => Покупка %s лотов по цене %s (priceTV:%s). Время %s.", strategyTv.getName(), Math.abs(position.doubleValue()), printPrice, strategyTv.getPriceTv(), time);
                userCache.addLogs(text);
                try {
                    if (userCache.getUser().getChatId() != null && changingStrategyTv.getConsumer().contains("telegram")) {
                        telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
                    }
                } catch (Exception ignored) {
                }
            }
            if (strategyTv.getDirection().equals("sell")) {
                if (position.compareTo(BigDecimal.ZERO) < 0) {
                    changingStrategyTv.setCurrentPosition(changingStrategyTv.getCurrentPosition().add(position));
                } else {
                    changingStrategyTv.setCurrentPosition(changingStrategyTv.getCurrentPosition().subtract(position));
                }
//                for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
//                    changingStrategyTv.addOrder(new Order(executionPrice, minLot, strategyTv.getDirection(), time, orderLinkId));
//                }
                String text = String.format("%s => Продажа %s лотов по цене %s (priceTV:%s). Время %s.", strategyTv.getName(), Math.abs(position.doubleValue()), printPrice, strategyTv.getPriceTv(), time);
                userCache.addLogs(text);
                try {
                    if (userCache.getUser().getChatId() != null && changingStrategyTv.getConsumer().contains("telegram")) {
                        telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
                    }
                } catch (Exception ignored) {
                }
            }
            if (strategyTv.getDirection().equals("hold")) {
                if (changingStrategyTv.getCurrentPosition().compareTo(BigDecimal.ZERO) != 0) {
                    String text = String.format(changingStrategyTv.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0 ? "%s => Покупка %s лотов по цене %s (priceTV:%s). Время %s." : "%s => Продажа %s лотов по цене %s (priceTV:%s). Время %s.", strategyTv.getName(), Math.abs(position.doubleValue()), printPrice, strategyTv.getPriceTv(), time);
                    userCache.addLogs(text);
                    if (userCache.getUser().getChatId() != null && changingStrategyTv.getConsumer().contains("telegram")) {
                        telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
                    }
//                    for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
//                        changingStrategyTv.addOrder(new Order(executionPrice, minLot, changingStrategyTv.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0 ? "buy" : "sell", time, orderLinkId));
//                    }
                    if (changingStrategyTv.getCurrentPosition().compareTo(BigDecimal.ZERO) > 0) {
                        changingStrategyTv.setCurrentPosition(changingStrategyTv.getCurrentPosition().subtract(position));
                    } else {
                        changingStrategyTv.setCurrentPosition(changingStrategyTv.getCurrentPosition().add(position));
                    }
                }
            }
        }
        strategyTvList.set(Integer.parseInt(changingStrategyTv.getId()), changingStrategyTv);
        userCache.setStrategies(strategyTvList);
        USER_STORE.replace("Admin", userCache);

        sendMessageInSocket(userCache.getStrategies());
    }

    public void sendMessageInSocket(List<StrategyTv> strategyTvList) {
        Message message = new Message();
        message.setSenderName("server");
        message.setMessage(strategyTvList);
        message.setStatus(Status.JOIN);
        message.setCommand("strategy");
        message.setStatus(Status.JOIN);
        streamService.sendDataToUser(Set.of("Admin"), message);
    }

    public void setErrorAndSetOnUi(String mes) {
        Message message = new Message();
        message.setSenderName("server");
        message.setMessage(mes);
        message.setStatus(Status.JOIN);
        message.setCommand("error");
        message.setStatus(Status.JOIN);
        streamService.sendDataToUser(Set.of("Admin"), message);
    }
}
