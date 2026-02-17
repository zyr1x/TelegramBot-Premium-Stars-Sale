package ru.lewis.leykabot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fragment")
@Getter
@Setter
public class FragmentConfig {
    private String apiUrl;
    private String hash;
    private String cookies;
}
