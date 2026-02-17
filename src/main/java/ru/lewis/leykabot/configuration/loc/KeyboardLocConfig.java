package ru.lewis.leykabot.configuration.loc;

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
    private Map<String, Deposit> depositRubles;
    private Map<String, BuyStars> buyStars;

    @Getter
    @Setter
    public static class Deposit {
        private int amount;
        private String name;
    }

    @Getter
    @Setter
    public static class BuyStars {
        private int amount;
        private String name;
    }
}
