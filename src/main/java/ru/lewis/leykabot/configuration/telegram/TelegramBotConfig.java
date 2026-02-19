package ru.lewis.leykabot.configuration.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("telegram.bot")
public class TelegramBotConfig {
    private String token;
    private String name;

    public String getName() {
        return name;
    }

    public String getToken() {
        return token;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
