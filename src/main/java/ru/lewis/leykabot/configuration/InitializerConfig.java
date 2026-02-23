package ru.lewis.leykabot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.lewis.leykabot.configuration.telegram.TelegramBotConfig;

@Configuration
public class InitializerConfig {

    @Bean
    public TelegramClient telegramClient(TelegramBotConfig config) {
        return new OkHttpTelegramClient(config.getToken());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
