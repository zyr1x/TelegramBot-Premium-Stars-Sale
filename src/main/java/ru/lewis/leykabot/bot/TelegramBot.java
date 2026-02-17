package ru.lewis.leykabot.bot;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.TelegramConfig;
import ru.lewis.leykabot.service.FragmentStarsService;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.TonService;
import ru.lewis.leykabot.service.UserService;

@Component
@AllArgsConstructor
@Slf4j
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final TelegramService telegramService;
    private final ScreenManager screenManager;
    private final ClientMessageConfig clientMessageConfig;
    private final ButtonsLocConfig buttonsLocConfig;
    private final TelegramConfig telegramConfig;
    private final ScreenFactory screenFactory;
    private final UserService userService;
    private final FragmentStarsService fragmentStarsService;
    private final TonService tonService;

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            var callback = update.getCallbackQuery();
            var chatId = callback.getMessage().getChatId();
            var data = callback.getData();
            var messageId = callback.getMessage().getMessageId();

            try {
                telegramClient.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                        .callbackQueryId(callback.getId())
                        .build());
            } catch (Exception e) {
                log.error("Error answering callback", e);
            }

            screenManager.handleCallback(chatId, data, messageId);
            return;
        }

        if (!update.hasMessage() || update.getMessage() == null) {
            return;
        }

        var message = update.getMessage();

        if (!message.hasText() || message.getText() == null) {
            return;
        }

        var text = message.getText();
        var userId = message.getFrom().getId();
        var chatId = message.getChatId();

        if (text.equals("/start")) {
            if (!telegramService.isUserSubscribed(userId)) {
                telegramService.sendMessage(chatId, clientMessageConfig.getSubscribeChannel());
                return;
            }
            if (!userService.isUserExists(userId)) {
                userService.createUser(userId);
            }
            screenManager.createScreen(chatId, screenFactory.createStartScreen(chatId, userId));
            return;
        }

        screenManager.handleMessage(chatId, text);
    }
}
