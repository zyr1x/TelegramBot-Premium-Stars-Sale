package ru.lewis.leykabot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ton")
@Getter
@Setter
public class TonConfig {
    private Network network;
    private Wallet wallet;

    @Getter
    @Setter
    public static class Network {
        private String type;
        private Integer timeout;
        private Integer checkout;
    }

    @Getter
    @Setter
    public static class Wallet {
        private String apiKey;
        private String mnemonic;
    }
}