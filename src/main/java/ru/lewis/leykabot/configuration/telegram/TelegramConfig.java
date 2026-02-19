package ru.lewis.leykabot.configuration.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("telegram")
@Getter
@Setter
public class TelegramConfig {
    private String channelCheckSubscribeUrl;
    private String channelCheckSubscribe;
    private Long logChannelId;
    private Integer logChannelTopicId;
}
