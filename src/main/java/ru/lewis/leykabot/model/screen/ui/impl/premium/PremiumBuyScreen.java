package ru.lewis.leykabot.model.screen.ui.impl.premium;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.DevModeConfig;
import ru.lewis.leykabot.configuration.prem.PremiumConfig;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.loc.KeyboardLocConfig;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.TelegramService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PremiumBuyScreen extends AbstractScreen {

    private final ButtonsLocConfig    buttonsLocConfig;
    private final KeyboardLocConfig   keyboardLocConfig;
    private final ClientMessageConfig clientMessageConfig;
    private final TelegramService     telegramService;
    private final DevModeConfig       devModeConfig;
    private final PremiumConfig       premiumConfig;
    private final ScreenManager       screenManager;
    private final ScreenFactory       screenFactory;

    public PremiumBuyScreen(Long chatId, Long userId,
                            ButtonsLocConfig buttonsLocConfig,
                            KeyboardLocConfig keyboardLocConfig,
                            ClientMessageConfig clientMessageConfig,
                            TelegramService telegramService,
                            DevModeConfig devModeConfig,
                            PremiumConfig premiumConfig,
                            ScreenManager screenManager,
                            ScreenFactory screenFactory) {
        super(chatId, userId);
        this.buttonsLocConfig    = buttonsLocConfig;
        this.keyboardLocConfig   = keyboardLocConfig;
        this.clientMessageConfig = clientMessageConfig;
        this.telegramService     = telegramService;
        this.devModeConfig       = devModeConfig;
        this.premiumConfig       = premiumConfig;
        this.screenManager       = screenManager;
        this.screenFactory       = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        if (callback.equals("back")) {
            screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
            return;
        }

        if (devModeConfig.isEnable() && !devModeConfig.getWhitelist().contains(userId)) {
            telegramService.sendMessageAuto(chatId, clientMessageConfig.getDevelopmentMode());
            return;
        }

        // callback = ключ из keyboardLocConfig.getBuyPremium(), например "3", "6", "12"
        Map<String, KeyboardLocConfig.BuyPremium> premiumButtons = keyboardLocConfig.getBuyPremium();
        var buyPremium = premiumButtons.get(callback);
        if (buyPremium == null) return;

        int months      = buyPremium.getMonths();
        int rubles      = (int) Math.ceil(buyPremium.getBasePrice() * premiumConfig.getMarkup());

        screenManager.updateScreen(chatId,
                screenFactory.createSelectUserForBuyPremiumScreen(chatId, userId, months, rubles));
    }

    @Override
    public void handleMessage(String text, TelegramClient bot) {
        // Выбор срока — только через кнопки, ввод текста не нужен
    }

    @Override
    public String getText() {
        return clientMessageConfig.getBuyPremiumCommand();
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        Map<String, KeyboardLocConfig.BuyPremium> premiumButtons = keyboardLocConfig.getBuyPremium();

        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        int count = 0;

        for (Map.Entry<String, KeyboardLocConfig.BuyPremium> entry : premiumButtons.entrySet()) {
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
        backRow.add(InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getBack())
                .callbackData("back")
                .build());
        keyboard.add(backRow);

        return InlineKeyboardMarkup.builder().keyboard(keyboard).build();
    }
}