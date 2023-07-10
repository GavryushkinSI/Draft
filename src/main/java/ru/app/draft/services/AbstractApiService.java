package ru.app.draft.services;

import org.springframework.stereotype.Service;
import ru.app.draft.models.Message;
import ru.app.draft.models.Status;
import ru.app.draft.models.UserCache;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.stream.StreamProcessor;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static ru.app.draft.store.Store.USER_STORE;

@Service
public abstract class AbstractApiService {
    private final MarketDataStreamService streamService;

    protected AbstractApiService(MarketDataStreamService streamService) {
        this.streamService = streamService;
    }

    public void setPortfolioStream(InvestApi api) {
        var accounts = api.getUserService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();

        Consumer<Throwable> onErrorCallback = message -> {
            try {
                throw new Exception(message);
            } catch (Exception e) {
                setPortfolioStream(api);
            }
        };
        StreamProcessor<PortfolioStreamResponse> consumer = response -> {
            if (response.hasPing()) {
                //todo
            } else if (response.hasPortfolio()) {
                List<PortfolioPosition> portfolioPositions = response.getPortfolio().getPositionsList();
                UserCache userCache=USER_STORE.get("Admin");
                userCache.addPortfolio(portfolioPositions);
                Message message = new Message();
                message.setSenderName("server");
                message.setMessage(userCache.getPortfolios());
                message.setCommand("portfolio");
                message.setStatus(Status.JOIN);
                streamService.sendDataToUser(Set.of("Admin"), message);
            };
        };
        api.getOperationsStreamService().subscribePortfolio(consumer, onErrorCallback, mainAccount);
    }

    public void setOrdersStream(InvestApi api) {
        StreamProcessor<TradesStreamResponse> consumer = response -> {
            if (response.hasPing()) {
                //todo
            } else if (response.hasOrderTrades()) {
                //response
            }
        };

        Consumer<Throwable> onErrorCallback = message -> {
            try {
                throw new Exception(message);
            } catch (Exception e) {
                setOrdersStream(api);
            }
        };
        api.getOrdersStreamService().subscribeTrades(consumer, onErrorCallback);
    }
}
