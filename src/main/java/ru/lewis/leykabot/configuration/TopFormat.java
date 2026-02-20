package ru.lewis.leykabot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("top")
@Getter
@Setter
public class TopFormat {
    private int onePageLimit;
    private int maxLimit;

    private Section premium;
    private Section rubles;
    private Section stars;

    @Getter
    @Setter
    public static class Section {
        private String format;
        private String message;
    }
}