package ru.lewis.leykabot.model.dto.platega;

import lombok.Data;

@Data
public class PaymentGetStatusResponse {
    private String id;
    private PaymentStatus status;
}
