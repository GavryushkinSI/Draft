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


    public void sendSignal(Strategy strategy) {

    }

    abstract Object getPositionInfo();

    public synchronized void updateStrategyCache(List<Strategy> strategyList, Strategy strategy, Strategy changingStrategy, BigDecimal executionPrice, UserCache userCache, BigDecimal position, String time, Boolean ordersFromTv, String orderLinkId) {
        if (executionPrice != null) {
            var minLot = changingStrategy.getMinLot();
            String printPrice = CommonUtils.formatNumber(executionPrice, changingStrategy.getPriceScale());

            if (strategy.getDirection().equals("buy")) {
                changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition().add(position));
                for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
                    changingStrategy.addOrder(new Order(executionPrice, minLot, strategy.getDirection(), time, orderLinkId));
                }
                String text = String.format("%s => Покупка %s лотов по цене %s (priceTV:%s). Время %s.", strategy.getName(), Math.abs(position.doubleValue()), printPrice, strategy.getPriceTv(), time);
                userCache.addLogs(text);
                try {
                    if (userCache.getUser().getChatId() != null && changingStrategy.getConsumer().contains("telegram")) {
                        telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
                    }
                } catch (Exception ignored) {
                }
            }
            if (strategy.getDirection().equals("sell")) {
                if (position.compareTo(BigDecimal.ZERO) < 0) {
                    changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition().add(position));
                } else {
                    changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition().subtract(position));
                }
                for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
                    changingStrategy.addOrder(new Order(executionPrice, minLot, strategy.getDirection(), time, orderLinkId));
                }
                String text = String.format("%s => Продажа %s лотов по цене %s (priceTV:%s). Время %s.", strategy.getName(), Math.abs(position.doubleValue()), printPrice, strategy.getPriceTv(), time);
                userCache.addLogs(text);
                try {
                    if (userCache.getUser().getChatId() != null && changingStrategy.getConsumer().contains("telegram")) {
                        telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
                    }
                } catch (Exception ignored) {
                }
            }
            if (strategy.getDirection().equals("hold")) {
                if (changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) != 0) {
                    String text = String.format(changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0 ? "%s => Покупка %s лотов по цене %s (priceTV:%s). Время %s." : "%s => Продажа %s лотов по цене %s (priceTV:%s). Время %s.", strategy.getName(), Math.abs(position.doubleValue()), printPrice, strategy.getPriceTv(), time);
                    userCache.addLogs(text);
                    if (userCache.getUser().getChatId() != null && changingStrategy.getConsumer().contains("telegram")) {
                        telegramBotService.sendMessage(Long.parseLong(userCache.getUser().getChatId()), text);
                    }
                    for (int i = 1; i <= Math.abs(position.divide(minLot, RoundingMode.CEILING).doubleValue()); i++) {
                        changingStrategy.addOrder(new Order(executionPrice, minLot, changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) < 0 ? "buy" : "sell", time, orderLinkId));
                    }
                    if (changingStrategy.getCurrentPosition().compareTo(BigDecimal.ZERO) > 0) {
                        changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition().subtract(position));
                    } else {
                        changingStrategy.setCurrentPosition(changingStrategy.getCurrentPosition().add(position));
                    }
                }
            }
        }
        strategyList.set(Integer.parseInt(changingStrategy.getId()), changingStrategy);
        userCache.setStrategies(strategyList);
        USER_STORE.replace("Admin", userCache);

        sendMessageInSocket(userCache.getStrategies());
    }

    public void sendMessageInSocket(List<Strategy> strategyList) {
        Message message = new Message();
        message.setSenderName("server");
        message.setMessage(strategyList);
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
