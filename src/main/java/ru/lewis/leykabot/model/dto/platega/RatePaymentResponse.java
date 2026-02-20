package ru.lewis.leykabot.model.dto.platega;

import lombok.Data;

@Data
public class RatePaymentResponse {
    private String paymentMethod;
    private String currencyFrom;
    private String currencyTo;
    private float rate;
}
