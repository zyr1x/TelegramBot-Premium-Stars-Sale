package ru.lewis.leykabot.configuration.loc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("localization.log-message")
@Getter
@Setter
public class LogMessageConfig {
    private String createCode;
    private String transactionCreate;
    private String appDisable;
    private String appEnable;
    private String balanceInTonNotEnough;
    private String referralActivated;
}
