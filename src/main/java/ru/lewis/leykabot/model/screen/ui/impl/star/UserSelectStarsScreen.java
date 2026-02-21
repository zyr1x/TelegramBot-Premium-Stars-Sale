package ru.lewis.leykabot.model.screen.ui.impl.star;

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
import ru.lewis.leykabot.service.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UserSelectStarsScreen extends AbstractScreen {
    private final int rubles, stars;
    private final ClientMessageConfig clientMessageConfig;
    private final ButtonsLocConfig buttonsLocConfig;
    private final TelegramService telegramService;
    private final FragmentStarsService fragmentStarsService;
    private final ErrorMessageConfig errorMessageConfig;
    private final UserService userService;
    private final TonService tonService;
    private final StarsTransactionService starsTransactionService;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    private String username = "";
    private boolean isOther = false;

    public UserSelectStarsScreen(Long chatId, Long userId, int stars, int rubles,
                                 ClientMessageConfig clientMessageConfig,
                                 ButtonsLocConfig buttonsLocConfig,
                                 TelegramService telegramService,
                                 FragmentStarsService fragmentStarsService,
                                 ErrorMessageConfig errorMessageConfig,
                                 UserService userService,
                                 TonService tonService,
                                 StarsTransactionService starsTransactionService,
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
        this.userService = userService;
        this.tonService = tonService;
        this.starsTransactionService = starsTransactionService;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "yourself" -> {
                username = telegramService.getUsernameByUserId(userId);
                telegramService.sendMessageAuto(chatId, clientMessageConfig.getSelectYourself());
            }

            case "other" -> {
                isOther = true;
                telegramService.sendMessageAuto(chatId, clientMessageConfig.getSelectOther());
            }

            case "confirm" -> handleConfirm();

            case "back" -> screenManager.updateScreen(chatId, screenFactory.createBuyStarsScreen(chatId, userId));
        }
    }

    private void handleConfirm() {
        if (username.isBlank()) {
            telegramService.sendMessageAuto(chatId, errorMessageConfig.getUsernameNotSelected());
            return;
        }
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
                        telegramService.sendMessageAuto(chatId, MessageFormat.format(errorMessageConfig.getFragmentError(), transaction.getError()));
                        return;
                    }
                    var balanceUserOptional = userService.getBalance(userId);
                    int balance = balanceUserOptional.orElse(0);

                    if (balance < rubles) {
                        var replenish = rubles - balance + 1; // + 1 на всякий случай, комиссия блять за калькулятор
                        screenManager.updateScreen(chatId, screenFactory.createRublesReplenishScreen(chatId, userId, replenish));
                        return;
                    }
                    transaction.getTransaction().getMessages().forEach(message -> {
                        var response = tonService.send(message.getAddress(), message.getPayload(), message.getAmount());

                        response.thenAccept((sendResponse) -> {
                            var code = sendResponse.getCode();
                            var sendResponseMessage = sendResponse.getMessage();

                            if (code == 0) {
                                telegramService.sendMessageAuto(chatId, MessageFormat.format(clientMessageConfig.getThanksForPayment(), rubles));
                                starsTransactionService.create(userId, -rubles, stars);
                            } else {
                                telegramService.sendMessageAuto(chatId, MessageFormat.format(errorMessageConfig.getTransactionNotCreated(), code, sendResponseMessage));
                            }
                            screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
                        });
                    });

                    isOther = false;
                    username = "";
                });
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        if (!isOther) return;
        username = text.startsWith("@") ? text.substring(1) : text;
        isOther  = false;
        telegramService.sendMessageAuto(chatId,
                MessageFormat.format(clientMessageConfig.getIntroducedUsername(), username));
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
