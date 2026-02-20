package ru.lewis.leykabot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("platega")
@Getter
@Setter
public class PlategaConfig {
    private String api;
    private String merchantId;
    private Makeup makeup;

    @Getter
    @Setter
    public static class Makeup {
        private double sbpqr;
        private double card;
        private double crypto;
    }
}
