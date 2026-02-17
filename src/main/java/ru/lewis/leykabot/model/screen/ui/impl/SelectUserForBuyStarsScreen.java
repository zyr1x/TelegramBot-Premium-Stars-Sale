package ru.lewis.leykabot.model.screen.ui.impl;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.loc.ErrorMessageConfig;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.FragmentStarsService;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.TransactionService;
import ru.lewis.leykabot.service.UserService;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SelectUserForBuyStarsScreen extends AbstractScreen {
    private final int rubles, stars;
    private final ClientMessageConfig clientMessageConfig;
    private final ButtonsLocConfig buttonsLocConfig;
    private final TelegramService telegramService;
    private final FragmentStarsService fragmentStarsService;
    private final ErrorMessageConfig errorMessageConfig;
    private final TransactionService transactionService;
    private final UserService userService;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    private String username = "";
    private boolean isOther = false;

    public SelectUserForBuyStarsScreen(Long chatId, Long userId, int stars, int rubles,
                                       ClientMessageConfig clientMessageConfig,
                                       ButtonsLocConfig buttonsLocConfig,
                                       TelegramService telegramService,
                                       FragmentStarsService fragmentStarsService,
                                       ErrorMessageConfig errorMessageConfig,
                                       TransactionService transactionService,
                                       UserService userService,
                                       ScreenManager screenManager,
                                       ScreenFactory screenFactory) {
        super(chatId, userId);
        this.rubles = rubles;
        this.stars = stars;
        this.clientMessageConfig = clientMessageConfig;
        this.buttonsLocConfig = buttonsLocConfig;
        this.telegramService = telegramService;
        this.fragmentStarsService = fragmentStarsService;
        this.errorMessageConfig = errorMessageConfig;
        this.transactionService = transactionService;
        this.userService = userService;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "yourself": {
                username = telegramService.getUsernameByUserId(userId);
                telegramService.sendMessageAuto(chatId, clientMessageConfig.getSelectYourself());
                break;
            }
            case "other": {
                isOther = true;
                telegramService.sendMessageAuto(chatId, clientMessageConfig.getSelectOther());
                break;
            }
            case "confirm": {
                fragmentStarsService.searchRecipient(username, stars)
                        .thenCompose(found -> {
                            if (found.getError() != null || !found.isOk()) {
                                telegramService.sendMessageAuto(chatId, errorMessageConfig.getUsernameNotFound());
                                return CompletableFuture.completedFuture(null);
                            }
                            var recipient = found.getFound().getRecipient();
                            return fragmentStarsService.initBuy(recipient, stars);
                        })
                        .thenCompose(initResponse -> {
                            if (initResponse == null) return CompletableFuture.completedFuture(null);
                            if (initResponse.getError() != null && initResponse.getReq_id() == null) {
                                telegramService.sendMessageAuto(chatId, MessageFormat.format(errorMessageConfig.getFragmentError(), initResponse.getError()));
                                return CompletableFuture.completedFuture(null);
                            }
                            return fragmentStarsService.createTransaction(initResponse.getReq_id());
                        })
                        .thenAccept(transaction -> {
                            if (transaction == null) return;
                            if (transaction.getError() != null) {
                                telegramService.sendMessageAuto(chatId, MessageFormat.format(errorMessageConfig.getBuyStarsMethodError(), transaction.getError()));
                                return;
                            }

                            var balance = userService.getBalance(userId);
                            if (balance.isEmpty() || balance.get() < rubles) {
                                telegramService.sendMessageAuto(chatId, clientMessageConfig.getRublesNotEnough());
                                return;
                            }

                            telegramService.sendMessageAuto(chatId, MessageFormat.format(clientMessageConfig.getThanksForPayment(), stars, rubles));
                            transactionService.createPurchaseTransaction(userId, -rubles, stars);
                            screenManager.updateScreen(chatId, screenFactory.createProfileScreen(chatId, userId));

                            isOther = false;
                            username = "";
                        });
                break;
            }
            case "back": {
                screenManager.updateScreen(chatId, screenFactory.createBuyStarsScreen(chatId, userId));
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (isOther) {
            username = text;
            isOther = false;
            telegramService.sendMessageAuto(chatId, MessageFormat.format(clientMessageConfig.getIntroducedUsername(), text));
        }
    }

    @Override
    public String getText() {
        return MessageFormat.format(clientMessageConfig.getSelectUserForBuyStarsCommand(), stars, rubles);
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        InlineKeyboardButton yourselfButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getYourself())
                .callbackData("yourself")
                .build();
        row1.add(yourselfButton);

        InlineKeyboardButton otherButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getOther())
                .callbackData("other")
                .build();
        row1.add(otherButton);

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        InlineKeyboardButton confirmButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getConfirm())
                .callbackData("confirm")
                .build();
        row2.add(confirmButton);

        InlineKeyboardRow row3 = new InlineKeyboardRow();
        InlineKeyboardButton backButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getBack())
                .callbackData("back")
                .build();
        row3.add(backButton);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }
}
