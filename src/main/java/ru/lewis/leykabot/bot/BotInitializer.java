package ru.lewis.leykabot.bot;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.lewis.leykabot.configuration.GitCommitConfig;
import ru.lewis.leykabot.configuration.TopFormat;
import ru.lewis.leykabot.configuration.telegram.TelegramBotConfig;
import ru.lewis.leykabot.configuration.telegram.TelegramConfig;
import ru.lewis.leykabot.configuration.loc.LogMessageConfig;
import ru.lewis.leykabot.service.CodeService;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.TopService;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class BotInitializer {
    private final TelegramBot telegramBot;
    private final TelegramBotConfig config;
    private final TelegramService telegramService;
    private final LogMessageConfig logMessageConfig;
    private final TelegramConfig telegramConfig;
    private final CodeService codeService;
    private final GitCommitConfig gitCommitConfig;
    private final TopService topService;
    private final TopFormat topFormat;

    private volatile boolean initialized = false;

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        if (initialized) return;
        initialized = true;

        CompletableFuture.allOf(
                codeService.warmUpAllCodes(),
                topService.preloadAll(topFormat.getMaxLimit())
        ).thenAccept(_void -> {
            String deployMessage = MessageFormat.format(
                    logMessageConfig.getAppEnable(),
                    gitCommitConfig.getHash(),
                    gitCommitConfig.getMessage(),
                    gitCommitConfig.getAuthor()
            );

            telegramService.sendMessageToTopic(
                    telegramConfig.getLogChannelId(),
                    telegramConfig.getLogChannelTopicId(),
                    deployMessage
            );

            try {
                TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
                botsApplication.registerBot(config.getToken(), telegramBot);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
