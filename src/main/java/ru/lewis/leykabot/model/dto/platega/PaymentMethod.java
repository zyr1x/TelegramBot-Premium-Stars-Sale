package ru.lewis.leykabot.model.dto.platega;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    SBPQR(2),
    CARD(10),
    CARD_ACQUIRING(11),
    INTERNATIONAL_ACQUIRING(12),
    CRYPTO(13)
    ;

    private final int  id;

    PaymentMethod(int  id) {
        this.id = id;
    }
}
