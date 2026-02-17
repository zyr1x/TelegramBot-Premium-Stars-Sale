package ru.lewis.leykabot.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties("dev-mode")
@Getter
@Setter
public class DevModeConfig {
    private boolean enable;
    private List<Long> whitelist;
}
