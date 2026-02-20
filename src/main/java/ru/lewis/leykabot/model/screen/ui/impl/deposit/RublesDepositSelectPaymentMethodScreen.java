package ru.lewis.leykabot.model.screen.ui.impl.deposit;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.loc.KeyboardLocConfig;
import ru.lewis.leykabot.model.dto.platega.PaymentCreateResponse;
import ru.lewis.leykabot.model.dto.platega.PaymentMethod;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.PlategaService;
import ru.lewis.leykabot.service.TelegramService;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RublesDepositSelectPaymentMethodScreen extends AbstractScreen {
    private final int rubles;

    private final TelegramService telegramService;
    private final ButtonsLocConfig buttonsLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final KeyboardLocConfig keyboardLocConfig;
    private final PlategaService plategaService;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    public RublesDepositSelectPaymentMethodScreen(Long chatId, Long userId, int rubles,
                                                  TelegramService telegramService,
                                                  ButtonsLocConfig buttonsLocConfig,
                                                  ClientMessageConfig clientMessageConfig,
                                                  KeyboardLocConfig keyboardLocConfig,
                                                  PlategaService plategaService,
                                                  ScreenManager screenManager,
                                                  ScreenFactory screenFactory) {
        super(chatId, userId);
        this.rubles = rubles;
        this.telegramService = telegramService;
        this.buttonsLocConfig = buttonsLocConfig;
        this.clientMessageConfig = clientMessageConfig;
        this.keyboardLocConfig = keyboardLocConfig;
        this.plategaService = plategaService;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        // проверка что этот еблан не спамит транзакциями
        var transactions = plategaService.getTransactions(userId);
        if (transactions != null && transactions.size() >= 5) {
            var links = transactions.stream()
                    .map(plategaService::getPaymentCreateResponse)
                    .filter(Objects::nonNull)
                    .map(PaymentCreateResponse::getRedirect)
                    .collect(java.util.stream.Collectors.joining("\n"));

            telegramService.sendMessageAuto(chatId, MessageFormat.format(clientMessageConfig.getPaymentCreateLimit(), links));
            screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
            return;
        }
        PaymentMethod paymentMethod = null;

        switch (callback) {
            case "sbpqr" -> paymentMethod = PaymentMethod.SBPQR;
            case "card" -> paymentMethod = PaymentMethod.CARD_ACQUIRING;
            case "crypto" -> paymentMethod = PaymentMethod.CRYPTO;
        }

        plategaService.createPayment(paymentMethod, rubles, userId).thenAccept(response -> {
            var link = response.getRedirect();
            telegramService.sendMessage(chatId, MessageFormat.format(clientMessageConfig.getRublesSuccessfullyCreatedPayment(), link));
        });

        screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
    }

    @Override
    public String getText() {
        return clientMessageConfig.getRublesDepositSelectPaymentMethodCommand();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        InlineKeyboardButton sbpqrButton = InlineKeyboardButton.builder()
                .text(keyboardLocConfig.getPaymentMethods().getSbpqr())
                .callbackData("sbpqr")
                .build();
        InlineKeyboardButton cardButton = InlineKeyboardButton.builder()
                .text(keyboardLocConfig.getPaymentMethods().getCard())
                .callbackData("card")
                .build();
        InlineKeyboardButton cryptoButton = InlineKeyboardButton.builder()
                .text(keyboardLocConfig.getPaymentMethods().getCrypto())
                .callbackData("crypto")
                .build();
        row1.add(sbpqrButton);
        row1.add(cardButton);
        row1.add(cryptoButton);

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
