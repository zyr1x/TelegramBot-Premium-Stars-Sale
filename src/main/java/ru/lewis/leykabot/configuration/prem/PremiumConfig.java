package ru.lewis.leykabot.configuration.prem;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "premium")
public class PremiumConfig {
    private double markup;
}

