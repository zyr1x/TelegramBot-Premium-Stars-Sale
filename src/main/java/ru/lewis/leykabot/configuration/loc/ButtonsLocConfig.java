package ru.lewis.leykabot.configuration.loc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("localization.buttons")
@Getter
@Setter
public class ButtonsLocConfig {
    private String support;
    private String links;
    private String deposit;
    private String back;
    private String profile;
    private String buyStars;
    private String yourself;
    private String other;
    private String confirm;
}
