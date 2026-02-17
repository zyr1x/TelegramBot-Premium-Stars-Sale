package ru.lewis.leykabot.model.screen.ui;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.service.TelegramService;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScreenManager {
    private final TelegramClient telegramClient;
    private final Map<Long, AbstractScreen> activeScreens = new ConcurrentHashMap<>();
    private final Map<Long, Integer> lastMessageIds = new ConcurrentHashMap<>();
    private final TelegramService telegramService;

    public ScreenManager(TelegramService telegramService, TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
        this.telegramService = telegramService;
    }

    /**
     * Создает новый экран и отправляет его в чат
     */
    public void createScreen(long chatId, AbstractScreen screen) {
        activeScreens.put(chatId, screen);
        screen.render(telegramClient);

        if (screen.getCurrentMessageId() != null) {
            lastMessageIds.put(chatId, screen.getCurrentMessageId());
        }
    }

    /**
     * Удаляет старое сообщение и отправляет новое
     */
    public void updateScreen(long chatId, AbstractScreen screen) {
        Integer messageId = lastMessageIds.get(chatId);

        if (messageId != null) {
            Integer newMessageId = telegramService.sendMessageAuto(
                    telegramClient, chatId, messageId, screen.getText(), screen.getKeyboard()
            );
            if (newMessageId != null) {
                lastMessageIds.put(chatId, newMessageId);
                screen.setCurrentMessageId(newMessageId);
            }
        } else {
            createScreen(chatId, screen);
        }

        activeScreens.put(chatId, screen);
    }

    /**
     * Получает текущий активный экран для чата
     */
    public AbstractScreen getActiveScreen(long chatId) {
        return activeScreens.get(chatId);
    }

    /**
     * Обрабатывает callback от пользователя
     */
    public void handleCallback(long chatId, String callbackData, Integer messageId) {
        if (activeScreens.isEmpty()) {
            telegramService.deleteMessage(chatId, messageId);
            return;
        }

        var activeScreen = activeScreens.get(chatId);
        if (activeScreen == null || !Objects.equals(activeScreen.getCurrentMessageId(), messageId)) {
            telegramService.deleteMessage(chatId, messageId);
            return;
        }

        if (messageId != null) {
            activeScreen.setCurrentMessageId(messageId);
            lastMessageIds.put(chatId, messageId);
        }
        activeScreen.handleCallback(callbackData, telegramClient);
    }

    /**
     * Обрабатывает текстовое сообщение от пользователя
     */
    public void handleMessage(long chatId, String text) {
        AbstractScreen screen = activeScreens.get(chatId);
        if (screen != null) {
            screen.handleMessage(text, telegramClient);
        }
    }

    /**
     * Удаляет экран из памяти
     */
    public void removeScreen(long chatId) {
        activeScreens.remove(chatId);
        lastMessageIds.remove(chatId);
    }

    /**
     * Проверяет, есть ли активный экран для чата
     */
    public boolean hasActiveScreen(long chatId) {
        return activeScreens.containsKey(chatId);
    }
}