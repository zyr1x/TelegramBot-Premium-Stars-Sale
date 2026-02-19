package ru.lewis.leykabot.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "premium")
public class PremiumConfig {

    /**
     * Наценка на базовую цену Premium.
     * Итоговая цена = basePrice * markup
     */
    private double markup;
}

