package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.DevModeConfig;
import ru.lewis.leykabot.configuration.StarsConfig;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.loc.ErrorMessageConfig;
import ru.lewis.leykabot.configuration.loc.KeyboardLocConfig;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.TransactionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuyStarsScreen extends AbstractScreen {
    private final ButtonsLocConfig buttonsLocConfig;
    private final KeyboardLocConfig keyboardLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final TransactionService transactionService;
    private final ErrorMessageConfig errorMessageConfig;
    private final TelegramService telegramService;
    private final DevModeConfig devModeConfig;
    private final StarsConfig starsConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    private boolean isExpectationMessage;
    public BuyStarsScreen(Long chatId, Long userId,
                          ButtonsLocConfig buttonsLocConfig,
                          KeyboardLocConfig keyboardLocConfig,
                          ClientMessageConfig clientMessageConfig,
                          TransactionService transactionService,
                          ErrorMessageConfig errorMessageConfig,
                          TelegramService telegramService,
                          DevModeConfig devModeConfig,
                          StarsConfig starsConfig,
                          ScreenManager screenManager,
                          ScreenFactory screenFactory) {
        super(chatId, userId);
        this.buttonsLocConfig = buttonsLocConfig;
        this.keyboardLocConfig = keyboardLocConfig;
        this.clientMessageConfig = clientMessageConfig;
        this.transactionService = transactionService;
        this.errorMessageConfig = errorMessageConfig;
        this.telegramService = telegramService;
        this.devModeConfig = devModeConfig;
        this.starsConfig = starsConfig;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "back":
                screenManager.updateScreen(chatId, screenFactory.createProfileScreen(chatId, userId));
                break;
            default:
                break;
        }

        if (devModeConfig.isEnable() && !devModeConfig.getWhitelist().contains(userId)) {
            telegramService.sendMessage(chatId, clientMessageConfig.getDevelopmentMode());
            return;
        }
        Map<String, KeyboardLocConfig.BuyStars> starButtons = keyboardLocConfig.getBuyStars();
        var buyStar = starButtons.get(callback);

        if (buyStar == null) return;
        if (callback.equals("custom")) {
            isExpectationMessage = true;
            telegramService.sendMessage(chatId, clientMessageConfig.getStarBuyEnterSum());
            return;
        }
        int rublesMarkup = (int) Math.ceil(buyStar.getAmount() * starsConfig.getMarkup());
        screenManager.updateScreen(chatId, screenFactory.createSelectUserForBuyStarsScreen(chatId, userId, buyStar.getAmount(), rublesMarkup));
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!isExpectationMessage) return;

        try {
            var number = Integer.parseInt(text);

            if (number > 100000) {
                telegramService.sendMessage(chatId, errorMessageConfig.getStars().getMaxValue());
                return;
            } else if (number < 50) {
                telegramService.sendMessage(chatId, errorMessageConfig.getStars().getMinValue());
                return;
            }

            int rublesMarkup = (int) Math.ceil(number * starsConfig.getMarkup());
            screenManager.updateScreen(chatId, screenFactory.createSelectUserForBuyStarsScreen(chatId, userId, number, rublesMarkup));

            isExpectationMessage = false;
        } catch (NumberFormatException exception) {
            telegramService.sendMessage(chatId, errorMessageConfig.getNumberFormat());
        }
    }

    @Override
    public String getText() {
        return clientMessageConfig.getBuyStarsCommand();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        Map<String, KeyboardLocConfig.BuyStars> starsButtons = keyboardLocConfig.getBuyStars();

        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        int count = 0;

        for (Map.Entry<String, KeyboardLocConfig.BuyStars> entry : starsButtons.entrySet()) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(entry.getValue().getName())
                    .callbackData(entry.getKey())
                    .build();

            currentRow.add(button);
            count++;

            if (count % 2 == 0) {
                keyboard.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            keyboard.add(currentRow);
        }

        InlineKeyboardRow backRow = new InlineKeyboardRow();
        InlineKeyboardButton backButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getBack())
                .callbackData("back")
                .build();
        backRow.add(backButton);
        keyboard.add(backRow);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }
}
