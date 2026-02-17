package ru.lewis.leykabot.listener;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import ru.lewis.leykabot.configuration.TelegramConfig;
import ru.lewis.leykabot.configuration.loc.LogMessageConfig;
import ru.lewis.leykabot.service.TelegramService;

@Component
@AllArgsConstructor
public class ShutdownListener implements ApplicationListener<ContextClosedEvent> {
    private final TelegramService telegramService;
    private final TelegramConfig telegramConfig;
    private final LogMessageConfig logMessageConfig;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        telegramService.sendMessageToTopic(telegramConfig.getLogChannelId(), telegramConfig.getLogChannelTopicId(), logMessageConfig.getAppDisable());
    }
}
