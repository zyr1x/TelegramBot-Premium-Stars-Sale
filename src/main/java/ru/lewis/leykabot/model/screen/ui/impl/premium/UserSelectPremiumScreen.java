package ru.lewis.leykabot.model.screen.ui.impl.premium;

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
import ru.lewis.leykabot.service.FragmentPremiumService;
import ru.lewis.leykabot.service.PremiumTransactionService;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.TonService;
import ru.lewis.leykabot.service.UserService;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UserSelectPremiumScreen extends AbstractScreen {
    private final int months;
    private final int rubles;

    private final ClientMessageConfig    clientMessageConfig;
    private final ButtonsLocConfig       buttonsLocConfig;
    private final ErrorMessageConfig     errorMessageConfig;
    private final TelegramService        telegramService;
    private final FragmentPremiumService fragmentPremiumService;
    private final PremiumTransactionService premiumTransactionService;
    private final UserService            userService;
    private final TonService             tonService;
    private final ScreenManager          screenManager;
    private final ScreenFactory          screenFactory;

    private String  username = "";
    private boolean isOther  = false;

    public UserSelectPremiumScreen(Long chatId, Long userId,
                                   int months, int rubles,
                                   ClientMessageConfig clientMessageConfig,
                                   ButtonsLocConfig buttonsLocConfig,
                                   ErrorMessageConfig errorMessageConfig,
                                   TelegramService telegramService,
                                   FragmentPremiumService fragmentPremiumService,
                                   PremiumTransactionService premiumTransactionService,
                                   UserService userService,
                                   TonService tonService,
                                   ScreenManager screenManager,
                                   ScreenFactory screenFactory) {
        super(chatId, userId);
        this.months                   = months;
        this.rubles                   = rubles;
        this.clientMessageConfig      = clientMessageConfig;
        this.buttonsLocConfig         = buttonsLocConfig;
        this.errorMessageConfig       = errorMessageConfig;
        this.telegramService          = telegramService;
        this.fragmentPremiumService   = fragmentPremiumService;
        this.premiumTransactionService = premiumTransactionService;
        this.userService              = userService;
        this.tonService               = tonService;
        this.screenManager            = screenManager;
        this.screenFactory            = screenFactory;
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

            case "back" ->
                    screenManager.updateScreen(chatId, screenFactory.createBuyPremiumScreen(chatId, userId));
        }
    }

    private void handleConfirm() {
        if (username.isBlank()) {
            telegramService.sendMessageAuto(chatId, errorMessageConfig.getUsernameNotSelected());
            return;
        }
        fragmentPremiumService.searchRecipient(username, months)
                .thenCompose(found -> {
                    if (found == null || !found.isOk() || found.getFound() == null) {
                        telegramService.sendMessageAuto(chatId, errorMessageConfig.getUsernameNotFound());
                        return CompletableFuture.completedFuture(null);
                    }
                    String recipient = found.getFound().getRecipient();
                    return fragmentPremiumService.initBuy(recipient, months);
                })

                .thenCompose(initResponse -> {
                    if (initResponse == null) return CompletableFuture.completedFuture(null);
                    if (initResponse.getError() != null || initResponse.getReq_id() == null) {
                        telegramService.sendMessageAuto(chatId,
                                MessageFormat.format(errorMessageConfig.getFragmentError(), initResponse.getError()));
                        return CompletableFuture.completedFuture(null);
                    }
                    return fragmentPremiumService.createTransaction(initResponse.getReq_id());
                })

                .thenAccept(txResponse -> {
                    if (txResponse == null) return;
                    if (txResponse.getError() != null) {
                        telegramService.sendMessageAuto(chatId,
                                MessageFormat.format(errorMessageConfig.getFragmentError(), txResponse.getError()));
                        return;
                    }
                    txResponse.getTransaction().getMessages().forEach(message -> {
                        var balanceUserOptional = userService.getBalance(userId);
                        int balance = balanceUserOptional.orElse(0);

                        if (balance < rubles) {
                            var replenish = rubles - balance + 1; // + 1 на всякий случай, комиссия блять за калькулятор
                            screenManager.updateScreen(chatId, screenFactory.createRublesReplenishScreen(chatId, userId, replenish));
                            return;
                        }

                        tonService.send(message.getAddress(), message.getPayload(), message.getAmount())
                                .thenAccept(sendResponse -> {
                                    var code = sendResponse.getCode();

                                    if (code == 0) {
                                        premiumTransactionService.create(
                                                userId,
                                                rubles,
                                                months
                                        );
                                        telegramService.sendMessageAuto(chatId,
                                                MessageFormat.format(
                                                        clientMessageConfig.getThanksForPayment(),
                                                        rubles));
                                    } else {
                                        telegramService.sendMessageAuto(chatId,
                                                MessageFormat.format(
                                                        errorMessageConfig.getTransactionNotCreated(),
                                                        code, sendResponse.getMessage()));
                                    }

                                    screenManager.updateScreen(chatId,
                                            screenFactory.createStartScreen(chatId, userId));
                                });
                    });

                    isOther  = false;
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
        return MessageFormat.format(
                clientMessageConfig.getSelectUserForBuyPremiumCommand(), months, rubles);
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getYourself())
                .callbackData("yourself")
                .build());
        row1.add(InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getOther())
                .callbackData("other")
                .build());

        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getConfirm())
                .callbackData("confirm")
                .build());

        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getBack())
                .callbackData("back")
                .build());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}