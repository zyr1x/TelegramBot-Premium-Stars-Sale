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
import ru.lewis.leykabot.service.TransactionService;
import ru.lewis.leykabot.service.UserService;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class ProfileScreen extends AbstractScreen {
    private final ButtonsLocConfig buttonsLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;
    private final TelegramService telegramService;
    private final UserService userService;
    private final TransactionService transactionService;

    public ProfileScreen(Long chatId, Long userId, ButtonsLocConfig buttonsLocConfig,
                         ClientMessageConfig clientMessageConfig,
                         ScreenManager screenManager,
                         TelegramService telegramService,
                         UserService userService,
                         TransactionService transactionService,
                         ScreenFactory screenFactory) {
        super(chatId, userId);
        this.buttonsLocConfig = buttonsLocConfig;
        this.clientMessageConfig = clientMessageConfig;
        this.screenManager = screenManager;
        this.telegramService = telegramService;
        this.userService = userService;
        this.transactionService = transactionService;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "back": {
                screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
                break;
            }
            case "deposit": {
                screenManager.updateScreen(chatId, screenFactory.createDepositRublesScreen(chatId, userId));
                break;
            }
            case "buy-stars": {
                screenManager.updateScreen(chatId, screenFactory.createBuyStarsScreen(chatId, userId));
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        InlineKeyboardButton backButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getBack())
                .callbackData("back")
                .build();
        row1.add(backButton);

        InlineKeyboardButton depositButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getDeposit())
                .callbackData("deposit")
                .build();
        row1.add(depositButton);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        InlineKeyboardButton buyStarsButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getBuyStars())
                .callbackData("buy-stars")
                .build();
        row2.add(buyStarsButton);

        keyboard.add(row1);
        keyboard.add(row2);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }

    @Override
    public String getText() {
        return MessageFormat.format(clientMessageConfig.getProfileCommand(),
                userService.getBalance(userId).orElse(0),
                transactionService.getTransactionCount(userId),
                transactionService.getAllTimeStats(userId).getAverageStarsPerTransaction(),
                telegramService.getUsernameByUserId(userId));
    }
}
