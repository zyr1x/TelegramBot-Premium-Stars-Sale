package ru.lewis.leykabot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.TelegramConfig;

@Service
public class TelegramService {
    public static final int MAX_MESSAGE_LENGTH = 4000;

    private final TelegramClient telegramClient;
    private final TelegramConfig telegramConfig;

    public TelegramService(TelegramClient telegramClient, TelegramConfig telegramConfig) {
        this.telegramClient = telegramClient;
        this.telegramConfig = telegramConfig;
    }

    public String getUsernameByUserId(Long userId) {
        try {
            GetChat getChat = GetChat.builder()
                    .chatId(userId)
                    .build();

            Chat chat = telegramClient.execute(getChat);

            String username = chat.getUserName();

            if (username != null && !username.isEmpty()) {
                return "@" + username;
            } else {
                return chat.getFirstName() != null ? chat.getFirstName() : "User";
            }

        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isUserSubscribed(Long userId) {
        try {
            GetChatMember getChatMember = GetChatMember.builder()
                    .chatId(telegramConfig.getChannelCheckSubscribe())
                    .userId(userId)
                    .build();

            ChatMember chatMember = telegramClient.execute(getChatMember);
            String status = chatMember.getStatus();

            return "member".equals(status) ||
                    "administrator".equals(status) ||
                    "creator".equals(status);
        } catch (TelegramApiException e) {
            return false;
        }
    }

    public void deleteMessage(long chatId, int messageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(chatId), messageId);
            telegramClient.execute(deleteMessage);
        } catch (TelegramApiException exception) {
            exception.printStackTrace();
        }
    }

    public void log(String text) {
        sendMessageToTopic(telegramConfig.getLogChannelId(), telegramConfig.getLogChannelTopicId(), text);
    }

    public void log(String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        sendMessageToTopic(telegramConfig.getLogChannelId(), telegramConfig.getLogChannelTopicId(), text, inlineKeyboardMarkup);
    }

    public void sendMessageToTopic(Long chatId, Integer topicId, String text) {
        sendMessageToTopic(chatId, topicId, text, null);
    }

    public void sendMessageToTopic(Long chatId, Integer topicId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        try {
            var builder = SendMessage.builder()
                    .chatId(chatId)
                    .messageThreadId(topicId)
                    .text(text);

            if (inlineKeyboardMarkup != null) {
                builder.replyMarkup(inlineKeyboardMarkup);
            }
            builder.parseMode("HTML");

            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public Message sendMessage(Long chatId, String text) {
        return sendMessage(chatId, text, null);
    }

    public Message sendMessage(Long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        if (text.length() > MAX_MESSAGE_LENGTH) {
            text = text.substring(0, MAX_MESSAGE_LENGTH - 50) + "\n\n... (сообщение обрезано)";
        }

        var builder = SendMessage.builder()
                .chatId(chatId)
                .text(text);

        if (inlineKeyboardMarkup != null) {
            builder.replyMarkup(inlineKeyboardMarkup);
        }
        builder.parseMode("HTML");

        try {
            return telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void editMessage(TelegramClient bot, Long chatId, Integer messageId, String text) {
        editMessage(bot, chatId, messageId, text,null);
    }

    public void editMessage(TelegramClient bot, Long chatId, Integer messageId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        if (text.length() > MAX_MESSAGE_LENGTH) {
            text = text.substring(0, MAX_MESSAGE_LENGTH - 50) + "\n\n... (сообщение обрезано)";
        }

        var builder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text);

        if (inlineKeyboardMarkup != null) {
            builder.replyMarkup(inlineKeyboardMarkup);
        }
        builder.parseMode("HTML");

        try {
            bot.execute(builder.build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
