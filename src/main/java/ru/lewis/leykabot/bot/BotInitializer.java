package ru.lewis.leykabot.bot;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import ru.lewis.leykabot.configuration.TelegramBotConfig;
import ru.lewis.leykabot.configuration.TelegramConfig;
import ru.lewis.leykabot.configuration.loc.LogMessageConfig;
import ru.lewis.leykabot.service.CodeService;
import ru.lewis.leykabot.service.TelegramService;

@Component
@Slf4j
@AllArgsConstructor
public class BotInitializer {
    private final TelegramBot telegramBot;
    private final TelegramBotConfig config;
    private final TelegramService telegramService;
    private final LogMessageConfig logMessageConfig;
    private final TelegramConfig telegramConfig;
    private final CodeService codeService;

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(config.getToken(), telegramBot);
            codeService.warmUpAllCodes();

            telegramService.sendMessageToTopic(telegramConfig.getLogChannelId(), telegramConfig.getLogChannelTopicId(), logMessageConfig.getAppEnable());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
