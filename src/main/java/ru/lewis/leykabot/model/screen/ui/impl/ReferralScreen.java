package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.UserService;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class ReferralScreen extends AbstractScreen {
    private final UserService userService;
    private final ButtonsLocConfig buttonsLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final TelegramService telegramService;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    public ReferralScreen(Long chatId, Long userId,
                          UserService userService,
                          ButtonsLocConfig buttonsLocConfig,
                          ClientMessageConfig clientMessageConfig,
                          TelegramService telegramService,
                          ScreenManager screenManager,
                          ScreenFactory screenFactory) {
        super(chatId, userId);
        this.userService = userService;
        this.buttonsLocConfig = buttonsLocConfig;
        this.clientMessageConfig = clientMessageConfig;
        this.telegramService = telegramService;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "create-referral": {
                var referralLinks = userService.getUserReferrals(userId);
                var link = "";

                if (!referralLinks.isEmpty()) {
                    var referralLink = referralLinks.get(0);
                    link = userService.hashToLink(referralLink.getHash());
                } else {
                    link = userService.createReferralLink(userId);
                }
                telegramService.sendMessageAuto(chatId, MessageFormat.format(clientMessageConfig.getReferralLinkMessage(), link));
                break;
            }
            case "back": {
                screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
                break;
            }
            default:
                break;
        }
    }

    @Override
    public String getText() {
        return clientMessageConfig.getCreateReferralCommand();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        InlineKeyboardButton createReferral = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getReferralSystem())
                .callbackData("create-referral")
                .build();
        row1.add(createReferral);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        InlineKeyboardButton backButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getBack())
                .callbackData("back")
                .build();
        row2.add(backButton);

        keyboard.add(row1);
        keyboard.add(row2);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }
}
