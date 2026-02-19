package ru.lewis.leykabot.configuration.loc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("localization.error-message")
@Getter
@Setter
public class ErrorMessageConfig {
    private String transactionNotCreated;
    private String buyStarsMethodError;
    private String fragmentError;
    private String usernameNotFound;
    private String numberFormat;
    private Rubles rubles;
    private Stars stars;

    // --- Premium ---
    private String buyPremiumMethodError;
    private String usernameNotSelected;

    @Getter
    @Setter
    public static class Stars {
        private String minValue;
        private String maxValue;
    }

    @Getter
    @Setter
    public static class Rubles {
        private String minValue;
        private String maxValue;
    }
}
