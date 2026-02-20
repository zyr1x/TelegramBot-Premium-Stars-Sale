package ru.lewis.leykabot.model.dto.platega;

import lombok.Data;

@Data
public class PaymentCreateResponse {
    private String paymentMethod;
    private String transactionId;
    private String redirect;
    private PaymentStatus status;
    private String expiresIn;
    private String code;
    private String message;
    private java.util.List<ProviderError> data;

    @Data
    public static class ProviderError {
        private String key;
        private String message;
    }
}
