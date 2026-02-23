package ru.lewis.leykabot.model.dto.rapira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RapiraRateResponse {
    @JsonProperty("data")
    private List<TickerDto> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TickerDto {
        @JsonProperty("symbol")
        private String symbol;

        @JsonProperty("open")
        private BigDecimal open;

        @JsonProperty("high")
        private BigDecimal high;

        @JsonProperty("low")
        private BigDecimal low;

        @JsonProperty("close")
        private BigDecimal close;

        @JsonProperty("chg")
        private BigDecimal chg;

        @JsonProperty("change")
        private BigDecimal change;

        @JsonProperty("fee")
        private BigDecimal fee;

        @JsonProperty("lastDayClose")
        private BigDecimal lastDayClose;

        @JsonProperty("usdRate")
        private BigDecimal usdRate;

        @JsonProperty("baseUsdRate")
        private BigDecimal baseUsdRate;

        @JsonProperty("askPrice")
        private BigDecimal askPrice;

        @JsonProperty("bidPrice")
        private BigDecimal bidPrice;

        @JsonProperty("baseCoinScale")
        private Integer baseCoinScale;

        @JsonProperty("coinScale")
        private Integer coinScale;

        @JsonProperty("quoteCurrencyName")
        private String quoteCurrencyName;

        @JsonProperty("baseCurrency")
        private String baseCurrency;

        @JsonProperty("quoteCurrency")
        private String quoteCurrency;
    }
}