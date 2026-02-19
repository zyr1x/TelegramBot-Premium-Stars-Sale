package ru.lewis.leykabot.model.dto.fragment.premium;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Ответ Fragment API на метод searchPremiumGiftRecipient.
 *
 * Структура аналогична StarsSearchResponse, но для Premium-подарков.
 */
@Data
public class PremiumSearchResponse {

    /** true, если запрос выполнен успешно */
    private boolean ok;

    /** Найденный получатель (присутствует при ok=true) */
    private FoundUser found;

    /** Сообщение об ошибке (присутствует при ok=false) */
    private String error;

    @Data
    public static class FoundUser {
        /** Внутренний идентификатор получателя, передаётся в initGiftPremiumRequest */
        private String recipient;

        /** Отображаемое имя пользователя */
        private String name;

        /** URL аватара */
        private String photo;

        /** Юзернейм в Telegram */
        @JsonProperty("username")
        private String username;
    }
}