package ru.lewis.leykabot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.TelegramConfig;

import java.io.File;
import java.net.URL;

@Service
public class TelegramService {
    public static final int MAX_MESSAGE_LENGTH = 4000;

    private final TelegramClient telegramClient;
    private final TelegramConfig telegramConfig;

    public TelegramService(TelegramClient telegramClient, TelegramConfig telegramConfig) {
        this.telegramClient = telegramClient;
        this.telegramConfig = telegramConfig;
    }

    // ─── Парсинг формата "ТЕКСТ;путь" ────────────────────────────────────────

    private String[] parseTextAndPath(String raw) {
        int idx = raw.indexOf(';');
        if (idx == -1) {
            return new String[]{raw, null};
        }
        String text = raw.substring(0, idx).trim();
        String path = raw.substring(idx + 1).trim();
        return new String[]{text, path.isEmpty() ? null : path};
    }

    /**
     * Ищет файл сначала как абсолютный/относительный путь на диске,
     * затем как ресурс в classpath — работает и из IDE, и из собранного jar.
     *
     * В yaml пиши путь относительно resources, например: images/welcome.png
     */
    private File resolveFile(String path) {
        // 1. Абсолютный или относительный путь на диске
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        // 2. Classpath (resources/images/welcome.png → "images/welcome.png")
        URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) {
            try {
                return new File(resource.toURI());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // ─── Отправка сообщения с авто-разбором формата ───────────────────────────

    public Message sendMessageAuto(Long chatId, String raw) {
        return sendMessageAuto(chatId, raw, null);
    }

    public Message sendMessageAuto(Long chatId, String raw, InlineKeyboardMarkup markup) {
        String[] parts = parseTextAndPath(raw);
        String text = parts[0];
        String path = parts[1];

        if (path != null) {
            File photo = resolveFile(path);
            if (photo != null) {
                return sendPhoto(chatId, photo, text, markup);
            }
        }
        return sendMessage(chatId, text, markup);
    }

    // ─── Редактирование с авто-разбором формата ──────────────────────────────

    public void editMessageAuto(TelegramClient bot, Long chatId, Integer messageId, String raw) {
        editMessageAuto(bot, chatId, messageId, raw, null);
    }

    /**
     * Если новое сообщение с фото — заменяем медиа через EditMessageMedia.
     * Если без фото — удаляем старое и отправляем текстовое,
     * т.к. Telegram не позволяет убрать фото из уже отправленного сообщения.
     *
     * @return новый messageId (если пересоздано) или старый (если просто отредактировано)
     */
    public Integer editMessageAuto(TelegramClient bot, Long chatId, Integer messageId, String raw, InlineKeyboardMarkup markup) {
        String[] parts = parseTextAndPath(raw);
        String text = parts[0];
        String path = parts[1];

        if (path != null) {
            File photoFile = resolveFile(path);
            if (photoFile != null) {
                // Пробуем отредактировать медиа. Если сообщение было текстовым — удаляем и пересылаем с фото
                try {
                    editMedia(bot, chatId, messageId, photoFile, text, markup);
                    return messageId;
                } catch (Exception e) {
                    deleteMessage(chatId, messageId);
                    Message sent = sendPhoto(chatId, photoFile, text, markup);
                    return sent != null ? sent.getMessageId() : null;
                }
            }
        }

        // Фото нет — удаляем старое и шлём чистое текстовое
        deleteMessage(chatId, messageId);
        Message sent = sendMessage(chatId, text != null ? text : "", markup);
        return sent != null ? sent.getMessageId() : null;
    }

    // ─── Замена фото + подписи через EditMessageMedia ─────────────────────────

    public void editMedia(TelegramClient bot, Long chatId, Integer messageId, File photo, String caption) {
        editMedia(bot, chatId, messageId, photo, caption, null);
    }

    public void editMedia(TelegramClient bot, Long chatId, Integer messageId, File photo, String caption, InlineKeyboardMarkup markup) {
        if (caption != null && caption.length() > MAX_MESSAGE_LENGTH) {
            caption = caption.substring(0, MAX_MESSAGE_LENGTH - 50) + "\n\n... (сообщение обрезано)";
        }

        InputMediaPhoto media = new InputMediaPhoto(photo, photo.getName());
        media.setParseMode("HTML");
        if (caption != null) {
            media.setCaption(caption);
        }

        var builder = EditMessageMedia.builder()
                .chatId(chatId)
                .messageId(messageId)
                .media(media);

        if (markup != null) {
            builder.replyMarkup(markup);
        }

        try {
            bot.execute(builder.build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Отправка фото ────────────────────────────────────────────────────────

    public Message sendPhoto(Long chatId, File photo, String caption) {
        return sendPhoto(chatId, photo, caption, null);
    }

    public Message sendPhoto(Long chatId, File photo, String caption, InlineKeyboardMarkup markup) {
        if (caption != null && caption.length() > MAX_MESSAGE_LENGTH) {
            caption = caption.substring(0, MAX_MESSAGE_LENGTH - 50) + "\n\n... (сообщение обрезано)";
        }

        var builder = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(photo))
                .parseMode("HTML");

        if (caption != null && !caption.isEmpty()) {
            builder.caption(caption);
        }
        if (markup != null) {
            builder.replyMarkup(markup);
        }

        try {
            return telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Отправка фото по fileId или URL */
    public Message sendPhoto(Long chatId, String fileIdOrUrl, String caption, InlineKeyboardMarkup markup) {
        if (caption != null && caption.length() > MAX_MESSAGE_LENGTH) {
            caption = caption.substring(0, MAX_MESSAGE_LENGTH - 50) + "\n\n... (сообщение обрезано)";
        }

        var builder = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(fileIdOrUrl))
                .parseMode("HTML");

        if (caption != null && !caption.isEmpty()) {
            builder.caption(caption);
        }
        if (markup != null) {
            builder.replyMarkup(markup);
        }

        try {
            return telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ─── Редактирование подписи к фото ────────────────────────────────────────

    public void editCaption(Long chatId, Integer messageId, String caption) {
        editCaption(chatId, messageId, caption, null);
    }

    public void editCaption(Long chatId, Integer messageId, String caption, InlineKeyboardMarkup markup) {
        if (caption != null && caption.length() > MAX_MESSAGE_LENGTH) {
            caption = caption.substring(0, MAX_MESSAGE_LENGTH - 50) + "\n\n... (сообщение обрезано)";
        }

        var builder = EditMessageCaption.builder()
                .chatId(chatId)
                .messageId(messageId)
                .parseMode("HTML");

        if (caption != null) {
            builder.caption(caption);
        }
        if (markup != null) {
            builder.replyMarkup(markup);
        }

        try {
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // ─── Остальные методы ─────────────────────────────────────────────────────

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
        editMessage(bot, chatId, messageId, text, null);
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
            throw new RuntimeException(e);
        }
    }
}