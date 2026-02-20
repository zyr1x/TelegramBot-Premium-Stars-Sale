package ru.lewis.leykabot.model.screen.ui.impl.top;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.TopFormat;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.model.Top;
import ru.lewis.leykabot.model.screen.ui.AbstractScreen;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.TopService;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class TopShowScreen extends AbstractScreen {
    private final Top top;
    private final int page;

    private final TelegramService telegramService;
    private final TopService topService;
    private final TopFormat topFormat;
    private final ButtonsLocConfig buttonsLocConfig;
    private final ScreenManager screenManager;
    private final ScreenFactory screenFactory;

    public TopShowScreen(Long chatId, Long userId, Top top, int page,
                         TelegramService telegramService,
                         TopFormat topFormat,
                         ButtonsLocConfig buttonsLocConfig,
                         TopService topService,
                         ScreenManager screenManager,
                         ScreenFactory screenFactory) {
        super(chatId, userId);
        this.telegramService = telegramService;
        this.top = top;
        this.page = page;
        this.topFormat = topFormat;
        this.buttonsLocConfig = buttonsLocConfig;
        this.topService = topService;
        this.screenManager = screenManager;
        this.screenFactory = screenFactory;
    }

    @Override
    public void handleCallback(String callback, TelegramClient bot) {
        switch (callback) {
            case "back" -> screenManager.updateScreen(chatId, screenFactory.createTopSelectScreen(chatId, userId));
            case "next-page" -> screenManager.updateScreen(chatId, screenFactory.createTopShowScreen(chatId, userId, top, page + 1));
            case "forward-page" -> screenManager.updateScreen(chatId, screenFactory.createTopShowScreen(chatId, userId, top, page - 1));
        }
    }

    @Override
    protected InlineKeyboardMarkup getKeyboard() {
        List<InlineKeyboardRow> keyboard = new ArrayList<>();

        var offset = page * topFormat.getOnePageLimit();
        var limit  = topFormat.getOnePageLimit();
        var topEntryList = getTopEntry(offset, limit);
        var section = getSection();

        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        int count = 0;

        for (TopService.TopEntry topEntry : topEntryList) {
            var button = InlineKeyboardButton.builder()
                    .text(MessageFormat.format(section.getFormat(), topEntry.rank(),
                            telegramService.getFullNameByUserId(topEntry.telegramId()),
                            topEntry.total()))
                    .callbackData(topEntry.rank() + ":" + topEntry.telegramId())
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
        InlineKeyboardRow changePageRow = new InlineKeyboardRow();

        if (page != 0) {
            InlineKeyboardButton forwardPageButton = InlineKeyboardButton.builder()
                    .text(buttonsLocConfig.getForward())
                    .callbackData("forward-page")
                    .build();
            changePageRow.add(forwardPageButton);
        }

        if (topEntryList.size() == topFormat.getOnePageLimit()) {
            InlineKeyboardButton nextPageButton = InlineKeyboardButton.builder()
                    .text(buttonsLocConfig.getNext())
                    .callbackData("next-page")
                    .build();

            changePageRow.add(nextPageButton);
        }


        InlineKeyboardRow backRow = new InlineKeyboardRow();
        InlineKeyboardButton backButton = InlineKeyboardButton.builder()
                .text(buttonsLocConfig.getBack())
                .callbackData("back")
                .build();
        backRow.add(backButton);

        if (!changePageRow.isEmpty()) keyboard.add(changePageRow);
        keyboard.add(backRow);

        return InlineKeyboardMarkup.builder()
                .keyboard(keyboard)
                .build();
    }

    @Override
    public String getText() {
        return getSection().getMessage();
    }

    private List<TopService.TopEntry> getTopEntry(int offset, int limit) {
        return switch (top) {
            case STARS   -> topService.getTopByStars(offset, limit);
            case RUBLES  -> topService.getTopByRubles(offset, limit);
            case PREMIUM -> topService.getTopByPremium(offset, limit);
        };
    }

    private TopFormat.Section getSection() {
        return switch (top) {
            case STARS -> topFormat.getStars();
            case RUBLES -> topFormat.getRubles();
            case PREMIUM -> topFormat.getPremium();
        };
    }
}
