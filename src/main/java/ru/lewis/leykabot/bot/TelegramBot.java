package ru.lewis.leykabot.bot;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.loc.ButtonsLocConfig;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.configuration.TelegramConfig;
import ru.lewis.leykabot.configuration.loc.LogMessageConfig;
import ru.lewis.leykabot.service.*;
import ru.lewis.leykabot.model.screen.ui.ScreenFactory;
import ru.lewis.leykabot.model.screen.ui.ScreenManager;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

@Component
@AllArgsConstructor
@Slf4j
public class TelegramBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final TelegramService telegramService;
    private final ScreenManager screenManager;
    private final ClientMessageConfig clientMessageConfig;
    private final ButtonsLocConfig buttonsLocConfig;
    private final TelegramConfig telegramConfig;
    private final ScreenFactory screenFactory;
    private final UserService userService;
    private final FragmentStarsService fragmentStarsService;
    private final TonService tonService;
    private final TransactionService transactionService;
    private final CodeService codeService;
    private final LogMessageConfig logMessageConfig;
    private final StarsTransactionService starsTransactionService;
    private final PremiumTransactionService premiumTransactionService;

    @Override
    public void consume(Update update) {
        if (update.hasCallbackQuery()) {
            var callback = update.getCallbackQuery();
            var chatId = callback.getMessage().getChatId();
            var data = callback.getData();
            var messageId = callback.getMessage().getMessageId();

            try {
                telegramClient.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                        .callbackQueryId(callback.getId())
                        .build());
            } catch (Exception e) {
                log.error("Error answering callback", e);
            }

            screenManager.handleCallback(chatId, data, messageId);
            return;
        }

        if (!update.hasMessage() || update.getMessage() == null) {
            return;
        }

        var message = update.getMessage();

        if (!message.hasText() || message.getText() == null) {
            return;
        }

        var text = message.getText();
        var userId = message.getFrom().getId();
        var chatId = message.getChatId();

        start(text, userId, chatId);

        screenManager.handleMessage(chatId, text);
    }

    private void start(String message, Long userId, Long chatId) {
        // command check
        if (message.startsWith("/start")) {
            // active referral

            if (message.contains(" ")) {
                var qrCode = message.split(" ")[1];

                if (userService.activateReferral(qrCode, userId)) {
                    var referralOwnerId = userService.getReferralOwner(qrCode).get();
                    var referralOwnerName = telegramService.getUsernameByUserId(referralOwnerId);
                    var referralActivatedName = telegramService.getUsernameByUserId(userId);

                    var referralAmountActivated = userService.getReferralActivationCount(referralOwnerId);

                    telegramService.sendMessage(chatId, clientMessageConfig.getReferralActivated());
                    telegramService.sendMessageToTopic(telegramConfig.getLogChannelId(), telegramConfig.getLogChannelTopicId(),
                            MessageFormat.format(logMessageConfig.getReferralActivated(),
                                    referralActivatedName, referralOwnerName, referralAmountActivated));
                }
            }

            // check sub
            if (!telegramService.isUserSubscribed(userId)) {
                screenManager.createScreen(chatId, screenFactory.createSubscribeChannelScreen(chatId, userId));
                return;
            }

            // save user id DB if not exists
            if (!userService.isUserExists(userId)) {
                userService.createUser(userId);
            }

            CompletableFuture.allOf(
                    premiumTransactionService.preload(userId),
                    starsTransactionService.preload(userId),
                    userService.warmUpAll(userId),
                    transactionService.preload(userId),
                    codeService.warmUpAll(userId)
            ).thenRun(() ->
                    screenManager.createScreen(chatId, screenFactory.createStartScreen(chatId, userId))
            );
        }
    }
}
