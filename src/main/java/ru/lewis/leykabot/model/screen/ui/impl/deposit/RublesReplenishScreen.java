package ru.lewis.leykabot.model.screen.ui.impl.deposit;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class RublesReplenishScreen extends AbstractScreen {
    private final  int shortageRubles;

    private final ButtonsLocConfig buttonsLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    public RublesReplenishScreen(Long chatId, Long userId, int shortageRubles,
                                 ButtonsLocConfig buttonsLocConfig,
                                 ClientMessageConfig clientMessageConfig,
                                 ScreenManager screenManager,
                                 ScreenFactory screenFactory) {
        super(chatId, userId);
        this.shortageRubles = shortageRubles;
        this.buttonsLocConfig = buttonsLocConfig;
        this.clientMessageConfig = clientMessageConfig;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "back" -> screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
            case "confirm" -> screenManager.updateScreen(chatId, screenFactory.createRublesDepositSelectPaymentMethodScreen(chatId, userId, shortageRubles));
        }
    }

    @Override
    public String getText() {
        return MessageFormat.format(clientMessageConfig.getRublesDepositAddCommand(), shortageRubles);
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        InlineKeyboardButton confirmButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getConfirm())
                .callbackData("confirm")
                .build();
        row1.add(confirmButton);

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
