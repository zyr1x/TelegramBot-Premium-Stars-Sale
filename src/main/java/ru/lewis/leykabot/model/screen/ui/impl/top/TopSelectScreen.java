package ru.lewis.leykabot.model.screen.ui.impl.top;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.model.Top;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;

import java.util.ArrayList;
import java.util.List;

public class TopSelectScreen extends AbstractScreen {
    private final ClientMessageConfig clientMessageConfig;
    private final ButtonsLocConfig buttonsLocConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    public TopSelectScreen(Long chatId, Long userId,
                           ClientMessageConfig clientMessageConfig,
                           ButtonsLocConfig buttonsLocConfig,
                           ScreenManager screenManager,
                           ScreenFactory screenFactory) {
        super(chatId, userId);
        this.clientMessageConfig = clientMessageConfig;
        this.buttonsLocConfig = buttonsLocConfig;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "premium-top" -> screenManager.updateScreen(chatId, screenFactory.createTopShowScreen(chatId, userId, Top.PREMIUM, 0));
            case "stars-top" -> screenManager.updateScreen(chatId, screenFactory.createTopShowScreen(chatId, userId, Top.STARS, 0));
            case "rubles-top" -> screenManager.updateScreen(chatId, screenFactory.createTopShowScreen(chatId, userId, Top.RUBLES, 0));
            case "back" -> screenManager.updateScreen(chatId, screenFactory.createStartScreen(chatId, userId));
        }
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        InlineKeyboardRow row1 = new InlineKeyboardRow();
        InlineKeyboardButton premiumTopButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getPremiumTop())
                .callbackData("premium-top")
                .build();
        InlineKeyboardButton starsTopButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getStarsTop())
                .callbackData("stars-top")
                .build();
        InlineKeyboardButton rublesTopButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getRublesTop())
                .callbackData("rubles-top")
                .build();
        row1.add(premiumTopButton);
        row1.add(starsTopButton);
        row1.add(rublesTopButton);

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

    @Override
    public String getText() {
        return clientMessageConfig.getTopCommand();
    }
}
