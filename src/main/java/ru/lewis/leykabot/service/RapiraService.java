package ru.lewis.leykabot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.lewis.leykabot.model.dto.rapira.RapiraRateResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class RapiraService {
    private static final String API_URL = "https://api.rapira.net/open/market/rates";
    private static final String CACHE_KEY = "tickers";
    private final Cache<String, List<RapiraRateResponse.TickerDto>> cache = buildCache();

    private final RestTemplate restTemplate;

    private <K, V> Cache<K, V> buildCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .build();
    }

    public CompletableFuture<List<RapiraRateResponse.TickerDto>> getRates() {
        return CompletableFuture.supplyAsync(() -> cache.get(CACHE_KEY, key -> fetchFromApi()));
    }

    public CompletableFuture<Double> getUsdtToRubRateWithMarkup() {
        return getRates().thenApply(tickers -> tickers.stream()
                .filter(t -> "USDT/RUB".equals(t.getSymbol()))
                .findFirst()
                .map(t -> {
                    BigDecimal rate = t.getClose()
                            .multiply(BigDecimal.valueOf(1.02))
                            .setScale(2, RoundingMode.HALF_UP);
                    return rate.doubleValue();
                })
                .orElseThrow(() -> new RuntimeException("USDT/RUB rate not found"))
        );
    }

    private List<RapiraRateResponse.TickerDto> fetchFromApi() {
        ResponseEntity<RapiraRateResponse> response =
                restTemplate.getForEntity(API_URL, RapiraRateResponse.class);

        return response.getBody().getData();
    }
}
