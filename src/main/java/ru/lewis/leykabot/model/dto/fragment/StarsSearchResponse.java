package ru.lewis.leykabot.model.dto.fragment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StarsSearchResponse {
    private boolean ok;
    private String error;
    private Found found;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Found {
        private String recipient;
        private String name;
    }
}
