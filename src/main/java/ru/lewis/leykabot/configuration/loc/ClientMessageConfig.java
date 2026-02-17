package ru.lewis.leykabot.configuration.loc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("localization.client-message")
@Getter
@Setter
public class ClientMessageConfig {
    private String developmentMode;
    private String depositEnterSum;
    private String starBuyEnterSum;
    private String supportCommand;
    private String linksCommand;
    private String profileCommand;
    private String startCommand;
    private String subscribeChannel;
    private String depositCommand;
    private String buyStarsCommand;
    private String selectUserForBuyStarsCommand;
    private String successfullyCreatedTransaction;
    private String thanksForPayment;
    private String selectYourself;
    private String selectOther;
    private String introducedUsername;
    private String rublesNotEnough;
}