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
import ru.lewis.leykabot.model.dto.fragment.star.StarsSearchResponse;
import ru.lewis.leykabot.model.dto.fragment.TransactionResponse;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FragmentStarsService {

    private final FragmentConfig config;
    private final RestTemplate restTemplate = new RestTemplate();

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

    @Async
    public CompletableFuture<StarsSearchResponse> searchRecipient(String username, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("method", "searchStarsRecipient");
                body.add("query", username);
                body.add("quantity", String.valueOf(quantity));

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, buildHeaders());

                ResponseEntity<StarsSearchResponse> response =
                        restTemplate.postForEntity(apiUrl(), entity, StarsSearchResponse.class);

                return response.getBody();
            } catch (Exception e) {
                log.error("Error searching recipient: {}", username, e);
                throw new RuntimeException("Failed to search recipient", e);
            }
        });
    }

    @Async
    public CompletableFuture<InitResponse> initBuy(String recipient, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("method", "initBuyStarsRequest");
                body.add("recipient", recipient);
                body.add("quantity", String.valueOf(quantity));

                HttpEntity<MultiValueMap<String, String>> entity =
                        new HttpEntity<>(body, buildHeaders());

                ResponseEntity<InitResponse> response =
                        restTemplate.postForEntity(apiUrl(), entity, InitResponse.class);

                return response.getBody();
            } catch (Exception e) {
                log.error("Error initializing buy for recipient: {}", recipient, e);
                throw new RuntimeException("Failed to initialize buy", e);
            }
        });
    }

    @Async
    public CompletableFuture<TransactionResponse> createTransaction(String reqId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("transaction", "1");
                body.add("method", "getBuyStarsLink");
                body.add("id", reqId);
                body.add("show_sender", "0");

                HttpEntity<MultiValueMap<String, String>> entity =
                        new HttpEntity<>(body, buildHeaders());

                ResponseEntity<TransactionResponse> response =
                        restTemplate.postForEntity(apiUrl(), entity, TransactionResponse.class);

                return response.getBody();
            } catch (Exception e) {
                log.error("Error creating transaction for reqId: {}", reqId, e);
                throw new RuntimeException("Failed to create transaction", e);
            }
        });
    }
}