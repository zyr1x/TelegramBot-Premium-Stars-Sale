package ru.lewis.leykabot.model.screen.ui;

import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Getter
@Setter
public abstract class AbstractScreen {
    protected Long chatId;
    protected Long userId;
    protected Integer currentMessageId;

    public AbstractScreen(Long chatId, Long userId) {
        this.chatId = chatId;
        this.userId = userId;
    }

    public void render(TelegramClient bot) {

    }

    public abstract void handleCallback(String callback, TelegramClient bot);

    public void handleMessage(String text, TelegramClient bot) {
        // По умолчанию ничего не делаем
    }

    public String getText() {
        return "Set text";
    }

    public String getImage() {
        return null;
    }

    protected abstract InlineKeyboardMarkup getKeyboard();
}