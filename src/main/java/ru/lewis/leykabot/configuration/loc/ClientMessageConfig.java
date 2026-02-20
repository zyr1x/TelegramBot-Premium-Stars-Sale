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
    private String createReferralCommand;
    private String selectUserForBuyStarsCommand;
    private String successfullyCreatedTransaction;
    private String thanksForPayment;
    private String selectYourself;
    private String selectOther;
    private String introducedUsername;
    private String rublesNotEnough;
    private String referralLinkMessage;
    private String referralActivated;

    // Premium
    private String buyPremiumCommand;
    private String selectUserForBuyPremiumCommand;
    private String selectYourselfPremium;
    private String selectOtherPremium;

    private String topCommand;

    private String rublesDepositSelectPaymentMethodCommand;
    private String rublesSuccessfullyCreatedPayment;
    private String paymentCreateLimit;
}