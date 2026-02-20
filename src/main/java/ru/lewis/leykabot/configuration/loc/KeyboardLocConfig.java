package ru.lewis.leykabot.configuration.loc;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties("localization.keyboard")
@Getter
@Setter
public class KeyboardLocConfig {
    private Map<String, Section> depositRubles;
    private Map<String, Section> buyStars;
    private Map<String, BuyPremium> buyPremium;
    private PaymentMethods paymentMethods;

    @Getter
    @Setter
    public static class Section {
        private int amount;
        private String name;
    }

    @Data
    public static class BuyPremium {
        private String name;
        private int months;
    }

    @Data
    public static class PaymentMethods {
        private String sbpqr;
        private String card;
        private String crypto;
    }
}
