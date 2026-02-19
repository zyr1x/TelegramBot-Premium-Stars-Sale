package ru.lewis.leykabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.lewis.leykabot.configuration.FragmentConfig;
import ru.lewis.leykabot.model.dto.fragment.InitResponse;
import ru.lewis.leykabot.model.dto.fragment.premium.PremiumSearchResponse;
import ru.lewis.leykabot.model.dto.fragment.TransactionResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Сервис для покупки Telegram Premium через Fragment API.
 *
 * Методы API (из fragment_seller.py):
 *   searchPremiumGiftRecipient   — поиск получателя
 *   initGiftPremiumRequest       — инициализация покупки
 *   getGiftPremiumLink           — получение ссылки на транзакцию (TON)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FragmentPremiumService {

    private static final String METHOD_SEARCH    = "searchPremiumGiftRecipient";
    private static final String METHOD_INIT      = "initGiftPremiumRequest";
    private static final String METHOD_GET_LINK  = "getGiftPremiumLink";

    private final FragmentConfig config;
    private final RestTemplate restTemplate = new RestTemplate();

    // ------------------------------------------------------------------ //
    //  Вспомогательные методы
    // ------------------------------------------------------------------ //

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("X-Requested-With", "XMLHttpRequest");
        headers.add("Cookie", config.getCookies());
        return headers;
    }

    private String apiUrl() {
        return config.getApiUrl() + "?hash=" + config.getHash();
    }

    // ------------------------------------------------------------------ //
    //  Публичные методы
    // ------------------------------------------------------------------ //

    /**
     * Шаг 1. Найти получателя по username и проверить, что он может получить Premium.
     *
     * @param username имя пользователя (например, "durov")
     * @param months   количество месяцев подписки (3 / 6 / 12)
     */
    @Async
    public CompletableFuture<PremiumSearchResponse> searchRecipient(String username, int months) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("method",   METHOD_SEARCH);
                body.add("query",    username);
                body.add("months",   String.valueOf(months));

                HttpEntity<MultiValueMap<String, String>> entity =
                        new HttpEntity<>(body, buildHeaders());

                ResponseEntity<PremiumSearchResponse> response =
                        restTemplate.postForEntity(apiUrl(), entity, PremiumSearchResponse.class);

                return response.getBody();
            } catch (Exception e) {
                log.error("Error searching Premium recipient: {}", username, e);
                throw new RuntimeException("Failed to search Premium recipient", e);
            }
        });
    }

    /**
     * Шаг 2. Инициализировать запрос на покупку Premium.
     *
     * @param recipient значение поля recipient из ответа {@link #searchRecipient}
     * @param months    количество месяцев подписки (3 / 6 / 12)
     */
    @Async
    public CompletableFuture<InitResponse> initBuy(String recipient, int months) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("method",    METHOD_INIT);
                body.add("recipient", recipient);
                body.add("months",    String.valueOf(months));

                HttpEntity<MultiValueMap<String, String>> entity =
                        new HttpEntity<>(body, buildHeaders());

                ResponseEntity<InitResponse> response =
                        restTemplate.postForEntity(apiUrl(), entity, InitResponse.class);

                return response.getBody();
            } catch (Exception e) {
                log.error("Error initializing Premium buy for recipient: {}", recipient, e);
                throw new RuntimeException("Failed to initialize Premium buy", e);
            }
        });
    }

    /**
     * Шаг 3. Получить данные TON-транзакции (адрес, сумма, payload) для оплаты.
     *
     * @param reqId значение req_id из ответа {@link #initBuy}
     */
    @Async
    public CompletableFuture<TransactionResponse> createTransaction(String reqId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("transaction",  "1");
                body.add("method",       METHOD_GET_LINK);
                body.add("id",           reqId);
                body.add("show_sender",  "0");

                HttpEntity<MultiValueMap<String, String>> entity =
                        new HttpEntity<>(body, buildHeaders());

                ResponseEntity<TransactionResponse> response =
                        restTemplate.postForEntity(apiUrl(), entity, TransactionResponse.class);

                return response.getBody();
            } catch (Exception e) {
                log.error("Error creating Premium transaction for reqId: {}", reqId, e);
                throw new RuntimeException("Failed to create Premium transaction", e);
            }
        });
    }
}