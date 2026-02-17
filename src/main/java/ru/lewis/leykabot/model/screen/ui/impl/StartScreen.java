package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.TelegramConfig;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;

import java.util.ArrayList;
import java.util.List;

public class StartScreen extends AbstractScreen {
    private final ClientMessageConfig clientMessageConfig;
    private final ButtonsLocConfig buttonsLocConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final TelegramConfig telegramConfig;
    private final TelegramService telegramService;

    public StartScreen(Long chatId, Long userId,
                       ClientMessageConfig clientMessageConfig,
                       ButtonsLocConfig buttonsLocConfig,
                       ScreenManager screenManager,
                       TelegramService telegramService,
                       TelegramConfig telegramConfig,
                       ScreenFactory screenFactory) {
        super(chatId, userId);
        this.clientMessageConfig = clientMessageConfig;
        this.buttonsLocConfig = buttonsLocConfig;
        this.screenManager = screenManager;
        this.telegramService = telegramService;
        this.screenFactory = screenFactory;
        this.telegramConfig = telegramConfig;
    }

    @Override
    public void render(TelegramClient bot) {
        Message message = telegramService.sendMessageAuto(chatId, getText(), getKeyboard());
        if (message != null) {
            this.currentMessageId = message.getMessageId();
        }
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "profile":
                screenManager.updateScreen(chatId, screenFactory.createProfileScreen(chatId, userId));
                break;
            case "support":
                screenManager.updateScreen(chatId, screenFactory.createSupportScreen(chatId, userId));
                break;
            case "links":
                screenManager.updateScreen(chatId, screenFactory.createLinksScreen(chatId, userId));
                break;
            default:
                break;
        }
    }

    @Override
    public String getText() {
        return clientMessageConfig.getStartCommand();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        InlineKeyboardButton profileButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getProfile())
                .callbackData("profile")
                .build();
        row1.add(profileButton);

        InlineKeyboardButton supportButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getSupport())
                .callbackData("support")
                .build();
        row1.add(supportButton);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        InlineKeyboardButton linksButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getLinks())
                .callbackData("links")
                .build();
        row2.add(linksButton);

        keyboard.add(row1);
        keyboard.add(row2);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }
}