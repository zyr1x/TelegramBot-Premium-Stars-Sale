package ru.lewis.leykabot.model.dto.platega;

import lombok.Data;

@Data
public class PaymentCreateResponse {
    private String paymentMethod;
    private String transactionId;
    private String redirect;
    private PaymentStatus status;
    private String expiresIn;
}
