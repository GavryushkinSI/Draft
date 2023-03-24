package ru.app.draft.services;

import liquibase.pro.packaged.i;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.app.draft.models.User;
import ru.app.draft.models.UserCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static ru.app.draft.store.Store.USER_STORE;

@Component
@Log4j2
public class TelegramBotService extends TelegramLongPollingBot {
    private final String name;
    private final String token;
    private final String owner;
    private final String START_MESSAGE = "Укажите свой логин при регистрации на сайте tview_parser...";
    private final String NOT_FOUND_USER = "Пользователь с таким именем не найден! Попробуйте ввести имя снова...";
    private final String HELP_MESSAGE = "Бот позволяет зарегистрироваться на получение уведомлений в телеграмм.";

    public TelegramBotService(@Value("${bot.name}") String botName, @Value("${bot.token}") String token, @Value("${bot.owner}") String owner) {
        this.name = botName;
        this.token = token;
        this.owner = owner;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "Старт"));
        listofCommands.add(new BotCommand("/help", "О боте"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return this.name;
    }

    @Override
    public String getBotToken() {
        return this.token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.getCallbackQuery() != null) {
            String x[] = update.getCallbackQuery().getData().split(" ");
            if (Objects.equals(x[0],"YES")) {
                Optional<UserCache> optionalUserCache = USER_STORE.values().stream().filter(i -> Objects.equals(i.getUser().getLogin(),x[2])).findFirst();
                if (optionalUserCache.isPresent()) {
                    UserCache userCache = optionalUserCache.get();
                    User user = userCache.getUser();
                    user.setChatId(x[1]);
                    userCache.setUser(user);
                    USER_STORE.replace(name, userCache);
                    sendMessage(Long.parseLong(x[1]), "Вы подписаны на уведомления!");
                }
            } else {
                sendMessage(Long.parseLong(x[1]), "OK!");
            }
        } else {
            switch (update.getMessage().getText()) {
                case "/start":
                    sendMessage(update.getMessage().getChatId(), START_MESSAGE);
                    break;
                case "/help":
                    sendMessage(update.getMessage().getChatId(), HELP_MESSAGE);
                    break;
                default:
                    registerUser(update.getMessage().getChatId(), update.getMessage().getText());
            }
        }
    }


    private void registerUser(long chatId, String name) {
        Optional<UserCache> optionalUserCache1 = USER_STORE.values().stream().filter(i -> Objects.equals(i.getUser().getLogin(),name)).findFirst();
        Optional<UserCache> optionalUserCache2 = USER_STORE.values().stream().filter(i -> Objects.equals(i.getUser().getChatId(),String.valueOf(chatId))).findFirst();
        if (optionalUserCache1.isEmpty()) {
            sendMessage(chatId, NOT_FOUND_USER);
            return;
        }
        if (optionalUserCache2.isPresent()) {
            sendMessage(Long.parseLong(optionalUserCache2.get().getUser().getChatId()), "Пользователь уже зарегистрирован на получение уведомлений в телеграмм.");
            return;
        }

        register(chatId, name);
    }

    private void register(long chatId, String name) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(String.format("%s, вы действительно хотите зарегистрироваться на получение уведомления о сделках в телеграмм?...", name));

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Yes");
        yesButton.setCallbackData("YES" + " " + chatId + " " + name);

        var noButton = new InlineKeyboardButton();

        noButton.setText("No");
        noButton.setCallbackData("NO" + " " + chatId);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
