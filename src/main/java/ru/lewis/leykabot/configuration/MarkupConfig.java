package ru.lewis.leykabot.configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("markup")
@Getter
@Setter
public class MarkupConfig {
    private float star;
    private float profit;
    private float platega;
}
