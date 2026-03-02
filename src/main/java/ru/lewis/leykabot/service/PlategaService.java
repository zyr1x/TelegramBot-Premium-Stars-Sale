package ru.lewis.leykabot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.lewis.leykabot.configuration.PlategaConfig;
import ru.lewis.leykabot.configuration.telegram.TelegramConfig;
import ru.lewis.leykabot.model.database.entity.PaymentEntity;
import ru.lewis.leykabot.model.dto.platega.*;
import ru.lewis.leykabot.repository.PaymentRepository;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PlategaService {
    private final static String API_URL = "https://app.platega.io/";
    private final static String API_URL_TRANSACTION = API_URL + "transaction/";
    private final static String API_URL_PAYMENT = API_URL_TRANSACTION + "process/";
    private final static String API_URL_RATES = API_URL + "rates/payment_method_rate";

    // кэш: userId -> список transactionId
    private final Cache<Long, List<String>> userTransactionsCache = buildCache();
    // кэш: transactionId -> PaymentCreateResponse
    private final Cache<String, PaymentCreateResponse> paymentResponseCache = buildCache();
    private final Cache<String, Integer> amountResponseCache = buildCache();

    private final PlategaConfig plategaConfig;
    private final TelegramConfig telegramConfig;
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    private <K, V> Cache<K, V> buildCache() {
        return Caffeine.newBuilder()
                .build();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-MerchantId", plategaConfig.getMerchantId());
        headers.add("X-Secret", plategaConfig.getApi());
        return headers;
    }

    // ─── загрузка из БД в кэш если кэш пустой ────────────────────────────────

    public CompletableFuture<Void> loadAllToCache() {
        return CompletableFuture.runAsync(() -> {
            var allPayments = paymentRepository.findAll();

            allPayments.forEach(entity -> {
                // загружаем userTransactionsCache
                var transactions = userTransactionsCache.getIfPresent(entity.getTelegramUserId());
                if (transactions == null) {
                    userTransactionsCache.put(entity.getTelegramUserId(),
                            new ArrayList<>(List.of(entity.getTransactionId())));
                } else {
                    transactions.add(entity.getTransactionId());
                }

                amountResponseCache.put(entity.getTransactionId(), entity.getAmount());

                // загружаем paymentResponseCache
                PaymentCreateResponse response = new PaymentCreateResponse();
                response.setTransactionId(entity.getTransactionId());
                response.setRedirect(entity.getRedirect());
                response.setPaymentMethod(entity.getPaymentMethod());
                response.setStatus(entity.getStatus());
                response.setCreatedAt(entity.getCreatedAt());
                paymentResponseCache.put(entity.getTransactionId(), response);
            });
        });
    }

    private List<String> loadTransactions(Long telegramUserId) {
        return userTransactionsCache.getIfPresent(telegramUserId);
    }

    // ─── методы ───────────────────────────────────────────────────────────────

    public CompletableFuture<RatePaymentResponse> getRateRubInUSDT() {
        return CompletableFuture.supplyAsync(() -> {
            String url = UriComponentsBuilder.fromUriString(API_URL_RATES)
                    .queryParam("merchantId", plategaConfig.getMerchantId())
                    .queryParam("paymentMethod", PaymentMethod.SBPQR.getId())
                    .queryParam("currencyFrom", "USDT")
                    .queryParam("currencyTo", "RUB")
                    .toUriString();

            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());

            ResponseEntity<RatePaymentResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, RatePaymentResponse.class);

            return response.getBody();
        });
    }

    public CompletableFuture<PaymentCreateResponse> createPayment(PaymentMethod paymentMethod, int amountRubles, Long telegramUserId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var makeup = switch (paymentMethod) {
                    case SBPQR -> plategaConfig.getMakeup().getSbpqr();
                    case CRYPTO -> plategaConfig.getMakeup().getCrypto();
                    default -> plategaConfig.getMakeup().getCard();
                };

                Map<String, Object> paymentDetails = new HashMap<>();
                paymentDetails.put("amount", amountRubles * makeup);
                paymentDetails.put("currency", "RUB");

                Map<String, Object> body = new HashMap<>();
                body.put("paymentMethod", paymentMethod.getId());
                body.put("paymentDetails", paymentDetails);
                body.put("description", MessageFormat.format("Пополнение баланса для {0} (ТГ айди), на сумму {1} ₽", telegramUserId, amountRubles));
                body.put("return", telegramConfig.getChannelCheckSubscribeUrl());
                body.put("failedUrl", telegramConfig.getChannelCheckSubscribeUrl());

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
                ResponseEntity<PaymentCreateResponse> response =
                        restTemplate.postForEntity(API_URL_PAYMENT, entity, PaymentCreateResponse.class);

                var now = LocalDateTime.now();
                var body2 = response.getBody();

                body2.setCreatedAt(now);
                String transactionId = body2.getTransactionId();

                PaymentEntity paymentEntity = new PaymentEntity();
                paymentEntity.setTransactionId(transactionId);
                paymentEntity.setTelegramUserId(telegramUserId);
                paymentEntity.setRedirect(body2.getRedirect());
                paymentEntity.setPaymentMethod(body2.getPaymentMethod());
                paymentEntity.setStatus(body2.getStatus());
                paymentEntity.setCreatedAt(now);
                paymentEntity.setAmount(amountRubles);
                paymentRepository.save(paymentEntity);

                // обновляем кэш
                var transactions = loadTransactions(telegramUserId);
                if (transactions == null) {
                    transactions = new ArrayList<>();
                    userTransactionsCache.put(telegramUserId, transactions);
                }
                transactions.add(transactionId);
                userTransactionsCache.put(telegramUserId, transactions);
                userTransactionsCache.put(telegramUserId, transactions);
                paymentResponseCache.put(transactionId, body2);
                amountResponseCache.put(transactionId, amountRubles);

                return body2;
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        });
    }

    public void deleteTransaction(String transactionId) {
        paymentRepository.deleteById(transactionId);

        paymentResponseCache.invalidate(transactionId);
        amountResponseCache.invalidate(transactionId);

        userTransactionsCache.asMap().forEach((userId, transactions) -> {
            transactions.remove(transactionId);
        });

        // отдельно чистим пустые
        userTransactionsCache.asMap().entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public CompletableFuture<PaymentStatus> checkStatus(String transactionId) {
        return CompletableFuture.supplyAsync(() -> {
            var userId = getUserIdByTransactionId(transactionId);
            if (userId == null) return PaymentStatus.NULL;

            var link = API_URL_TRANSACTION + transactionId;
            ResponseEntity<PaymentGetStatusResponse> response =
                    restTemplate.getForEntity(link, PaymentGetStatusResponse.class);

            return response.getBody().getStatus();
        });
    }

    public int getAmount(String transactionId) {
        var cached = amountResponseCache.getIfPresent(transactionId);
        if (cached != null) return cached;

        return paymentRepository.findById(transactionId)
                .map(PaymentEntity::getAmount)
                .orElse(0);
    }

    public Long getUserIdByTransactionId(String transactionId) {
        var fromCache = userTransactionsCache.asMap().entrySet().stream()
                .filter(entry -> entry.getValue().contains(transactionId))
                .map(Map.Entry::getKey)
                .findFirst();
        return fromCache.orElse(0L);
    }

    public List<String> getTransactions(Long telegramUserId) {
        var transactions = loadTransactions(telegramUserId);
        if (transactions == null) return List.of();

        new ArrayList<>(transactions).forEach(this::checkExpired);

        var updated = loadTransactions(telegramUserId);
        return updated != null ? List.copyOf(updated) : List.of();
    }

    private void checkExpired(String transaction) {
        var response = paymentResponseCache.getIfPresent(transaction);
        if (response == null) return; // добавь это

        var now = LocalDateTime.now();
        if (response.getCreatedAt().plusMinutes(30).isBefore(now)) {
            deleteTransaction(transaction);
        }
    }

    public PaymentCreateResponse getPaymentCreateResponse(String transactionId) {
        var cached = paymentResponseCache.getIfPresent(transactionId);
        if (cached != null) return cached;

        return paymentRepository.findById(transactionId).map(entity -> {
            PaymentCreateResponse r = new PaymentCreateResponse();
            r.setTransactionId(entity.getTransactionId());
            r.setRedirect(entity.getRedirect());
            r.setPaymentMethod(entity.getPaymentMethod());
            r.setStatus(entity.getStatus());
            return r;
        }).orElse(null);
    }
}