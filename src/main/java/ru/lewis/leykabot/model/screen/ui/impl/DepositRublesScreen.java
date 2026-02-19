package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.DevModeConfig;
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

public class DepositRublesScreen extends AbstractScreen {
    private final ButtonsLocConfig buttonsLocConfig;
    private final KeyboardLocConfig keyboardLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final TransactionService transactionService;
    private final ErrorMessageConfig errorMessageConfig;
    private final TelegramService telegramService;
    private final DevModeConfig devModeConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    private boolean isExpectationMessage;

    public DepositRublesScreen(Long chatId, Long userId,
                               ButtonsLocConfig buttonsLocConfig,
                               KeyboardLocConfig keyboardLocConfig,
                               ClientMessageConfig clientMessageConfig,
                               TransactionService transactionService,
                               ErrorMessageConfig errorMessageConfig,
                               TelegramService telegramService,
                               DevModeConfig devModeConfig,
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
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "back":
                screenManager.updateScreen(chatId, screenFactory.createProfileScreen(chatId, userId));
                return;
            default:
                break;
        }

        if (devModeConfig.isEnable() && !devModeConfig.getWhitelist().contains(userId)) {
            telegramService.sendMessageAuto(chatId, clientMessageConfig.getDevelopmentMode());
            return;
        }
        Map<String, KeyboardLocConfig.Deposit> depositButtons = keyboardLocConfig.getDepositRubles();
        var deposit = depositButtons.get(callback);

        if (deposit == null) return;
        if (callback.equals("custom")) {
            isExpectationMessage = true;
            telegramService.sendMessageAuto(chatId, clientMessageConfig.getDepositEnterSum());
            return;
        }
        transactionService.create(userId, deposit.getAmount());
        telegramService.sendMessageAuto(chatId, clientMessageConfig.getSuccessfullyCreatedTransaction());
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!isExpectationMessage) return;

        try {
            var number = Integer.parseInt(text);

            if (number > 100000) {
                telegramService.sendMessageAuto(chatId, errorMessageConfig.getRubles().getMaxValue());
                return;
            } else if (number < 10) {
                telegramService.sendMessageAuto(chatId, errorMessageConfig.getRubles().getMinValue());
                return;
            }
            transactionService.create(userId, number);
            telegramService.sendMessageAuto(chatId, clientMessageConfig.getSuccessfullyCreatedTransaction());
            isExpectationMessage = false;
        } catch (NumberFormatException exception) {
            telegramService.sendMessageAuto(chatId, errorMessageConfig.getNumberFormat());
        }
    }

    @Override
    public String getText() {
        return clientMessageConfig.getDepositCommand();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        Map<String, KeyboardLocConfig.Deposit> depositButtons = keyboardLocConfig.getDepositRubles();

        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        int count = 0;

        for (Map.Entry<String, KeyboardLocConfig.Deposit> entry : depositButtons.entrySet()) {
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
