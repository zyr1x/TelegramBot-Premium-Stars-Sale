package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.TelegramConfig;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;

public class SubscribeChannelScreen extends AbstractScreen {
    private final TelegramService telegramService;
    private final ClientMessageConfig clientMessageConfig;
    private final TelegramConfig telegramConfig;
    private final ButtonsLocConfig buttonsLocConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    public SubscribeChannelScreen(Long chatId, Long userId,
                                  TelegramService telegramService,
                                  ClientMessageConfig clientMessageConfig,
                                  TelegramConfig telegramConfig,
                                  ButtonsLocConfig buttonsLocConfig,
                                  ScreenManager screenManager,
                                  ScreenFactory screenFactory) {
        super(chatId, userId);
        this.telegramService = telegramService;
        this.clientMessageConfig = clientMessageConfig;
        this.telegramConfig = telegramConfig;
        this.buttonsLocConfig = buttonsLocConfig;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void render(TelegramClient bot) {
        telegramService.sendMessageAuto(chatId, getText(), getKeyboard());
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "check-sub": {
                if (!telegramService.isUserSubscribed(userId)) {
                    return;
                }
                screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
                break;
            }
            default:
                break;
        }
    }

    @Override
    public String getText() {
        return clientMessageConfig.getSubscribeChannel();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        InlineKeyboardButton goChannelButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getGoChannel())
                .url(telegramConfig.getChannelCheckSubscribeUrl())
                .build();
        row1.add(goChannelButton);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        InlineKeyboardButton checkSubButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getCheckSub())
                .callbackData("check-sub")
                .build();
        row2.add(checkSubButton);

        keyboard.add(row1);
        keyboard.add(row2);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }
}
