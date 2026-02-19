package ru.lewis.leykabot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lewis.leykabot.configuration.loc.LogMessageConfig;
import ru.lewis.leykabot.model.database.entity.StarsTransaction;
import ru.lewis.leykabot.model.database.entity.Transaction;
import ru.lewis.leykabot.repository.StarsTransactionRepository;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class StarsTransactionService {

    private final StarsTransactionRepository starsRepository;
    private final TransactionService         transactionService;
    private final TelegramService            telegramService;
    private final LogMessageConfig           logMessageConfig;

    // Кэш: telegramId → список звёздных транзакций DESC по дате
    private Cache<Long, List<StarsTransaction>> cache;

    @PostConstruct
    public void initCache() {
        cache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    // -------------------------------------------------------------------------
    // Кэш
    // -------------------------------------------------------------------------

    private List<StarsTransaction> warmUp(Long telegramId) {
        return cache.get(telegramId,
                id -> starsRepository.findByTelegramIdOrderByCreatedAtDesc(id));
    }

    /** Асинхронная подгрузка кэша — вызывается при /start. */
    public CompletableFuture<List<StarsTransaction>> preload(Long telegramId) {
        return CompletableFuture.supplyAsync(() -> warmUp(telegramId));
    }

    public List<StarsTransaction> refreshCache(Long telegramId) {
        List<StarsTransaction> fresh = starsRepository.findByTelegramIdOrderByCreatedAtDesc(telegramId);
        cache.put(telegramId, fresh);
        return fresh;
    }

    public void invalidateCache(Long telegramId) {
        cache.invalidate(telegramId);
    }

    // -------------------------------------------------------------------------
    // Запись
    // -------------------------------------------------------------------------

    /**
     * Создаёт звёздную транзакцию.
     * Сначала через {@link TransactionService#create} создаётся рублёвая транзакция,
     * затем к ней привязывается звёздная.
     *
     * @param telegramId        покупатель
     * @param amountRubles      стоимость в рублях (списывается с баланса)
     * @param amountStars       количество звёзд
     */
    @Transactional
    public StarsTransaction create(Long telegramId,
                                   Integer amountRubles,
                                   Integer amountStars) {

        // 1. Рублёвая транзакция — списание баланса
        Transaction parentTx = transactionService.create(telegramId, amountRubles);

        // 2. Звёздная транзакция — привязываем к рублёвой
        StarsTransaction starsTx = new StarsTransaction();
        starsTx.setTelegramId(telegramId);
        starsTx.setAmountStars(amountStars);
        starsTx.setTransaction(parentTx);

        StarsTransaction saved = starsRepository.save(starsTx);
        invalidateCache(telegramId);

        var tag = telegramService.getUsernameByUserId(telegramId);
        log.info("Звёздная транзакция #{} для {} {}: {} ⭐ → @{}",
                saved.getId(), tag, telegramId, amountStars);
        telegramService.log(MessageFormat.format(
                logMessageConfig.getStarsTransactionCreate(),
                tag, telegramId, amountStars));

        return saved;
    }

    // -------------------------------------------------------------------------
    // Чтение — всё из кэша
    // -------------------------------------------------------------------------

    public List<StarsTransaction> getAll(Long telegramId) {
        return warmUp(telegramId);
    }

    public List<StarsTransaction> getLast(Long telegramId, int limit) {
        return warmUp(telegramId).stream().limit(limit).toList();
    }

    public List<StarsTransaction> getByDateRange(Long telegramId, LocalDateTime from, LocalDateTime to) {
        return warmUp(telegramId).stream()
                .filter(t -> !t.getCreatedAt().isBefore(from) && !t.getCreatedAt().isAfter(to))
                .toList();
    }

    public long getTotalStars(Long telegramId) {
        return warmUp(telegramId).stream().mapToLong(StarsTransaction::getAmountStars).sum();
    }

    public long getCount(Long telegramId) {
        return warmUp(telegramId).size();
    }

    public boolean hasAny(Long telegramId) {
        return !warmUp(telegramId).isEmpty();
    }

    public StarsStats getMonthlyStats(Long telegramId) {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        return buildStats(warmUp(telegramId).stream()
                .filter(t -> t.getCreatedAt().isAfter(monthAgo))
                .toList());
    }

    public StarsStats getAllTimeStats(Long telegramId) {
        return buildStats(warmUp(telegramId));
    }

    /** Точечный запрос по ID — кэш не используется. */
    public StarsTransaction getById(Integer id) {
        return starsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Звёздная транзакция не найдена: " + id));
    }

    // -------------------------------------------------------------------------
    // Вспомогательное
    // -------------------------------------------------------------------------

    private StarsStats buildStats(List<StarsTransaction> list) {
        int totalStars = list.stream().mapToInt(StarsTransaction::getAmountStars).sum();
        return new StarsStats(list.size(), totalStars);
    }

    public record StarsStats(int count, int totalStars) {
        public double averageStars() {
            return count > 0 ? (double) totalStars / count : 0.0;
        }
    }
}