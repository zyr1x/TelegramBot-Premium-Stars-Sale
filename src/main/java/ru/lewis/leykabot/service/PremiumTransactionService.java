package ru.lewis.leykabot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lewis.leykabot.configuration.loc.LogMessageConfig;
import ru.lewis.leykabot.model.database.entity.PremiumTransaction;
import ru.lewis.leykabot.model.database.entity.Transaction;
import ru.lewis.leykabot.repository.PremiumTransactionRepository;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumTransactionService {

    private final PremiumTransactionRepository premiumRepository;
    private final TransactionService           transactionService;
    private final TelegramService              telegramService;
    private final LogMessageConfig             logMessageConfig;

    // Кэш: telegramId → список premium-транзакций DESC по дате
    private Cache<Long, List<PremiumTransaction>> cache;

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

    private List<PremiumTransaction> warmUp(Long telegramId) {
        return cache.get(telegramId,
                id -> premiumRepository.findByTelegramIdOrderByCreatedAtDesc(id));
    }

    /** Асинхронная подгрузка кэша — вызывается при /start. */
    public CompletableFuture<List<PremiumTransaction>> preload(Long telegramId) {
        return CompletableFuture.supplyAsync(() -> warmUp(telegramId));
    }

    public List<PremiumTransaction> refreshCache(Long telegramId) {
        List<PremiumTransaction> fresh = premiumRepository.findByTelegramIdOrderByCreatedAtDesc(telegramId);
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
     * Создаёт premium-транзакцию.
     * Сначала через {@link TransactionService#create} создаётся рублёвая транзакция,
     * затем к ней привязывается premium-транзакция.
     *
     * @param telegramId        покупатель
     * @param amountRubles      стоимость в рублях (списывается с баланса)
     * @param months            срок подписки: 3 / 6 / 12
     */
    @Transactional
    public PremiumTransaction create(Long telegramId,
                                     Integer amountRubles,
                                     Integer months) {

        // 1. Рублёвая транзакция — списание баланса
        Transaction parentTx = transactionService.create(telegramId, amountRubles);

        // 2. Premium-транзакция — привязываем к рублёвой
        PremiumTransaction premiumTx = new PremiumTransaction();
        premiumTx.setTelegramId(telegramId);
        premiumTx.setMonths(months);
        premiumTx.setTransaction(parentTx);

        PremiumTransaction saved = premiumRepository.save(premiumTx);
        invalidateCache(telegramId);

        var tag = telegramService.getUsernameByUserId(telegramId);
        log.info("Premium-транзакция #{} для {} {}: {} мес. → @{}",
                saved.getId(), tag, telegramId, months);
        telegramService.log(MessageFormat.format(
                logMessageConfig.getPremiumTransactionCreate(),
                tag, telegramId, months));

        return saved;
    }

    // -------------------------------------------------------------------------
    // Чтение — всё из кэша
    // -------------------------------------------------------------------------

    public List<PremiumTransaction> getAll(Long telegramId) {
        return warmUp(telegramId);
    }

    public List<PremiumTransaction> getLast(Long telegramId, int limit) {
        return warmUp(telegramId).stream().limit(limit).toList();
    }

    public List<PremiumTransaction> getByDateRange(Long telegramId, LocalDateTime from, LocalDateTime to) {
        return warmUp(telegramId).stream()
                .filter(t -> !t.getCreatedAt().isBefore(from) && !t.getCreatedAt().isAfter(to))
                .toList();
    }

    public long getTotalMonths(Long telegramId) {
        return warmUp(telegramId).stream().mapToLong(PremiumTransaction::getMonths).sum();
    }

    public long getCount(Long telegramId) {
        return warmUp(telegramId).size();
    }

    public boolean hasAny(Long telegramId) {
        return !warmUp(telegramId).isEmpty();
    }

    public PremiumStats getMonthlyStats(Long telegramId) {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        return buildStats(warmUp(telegramId).stream()
                .filter(t -> t.getCreatedAt().isAfter(monthAgo))
                .toList());
    }

    public PremiumStats getAllTimeStats(Long telegramId) {
        return buildStats(warmUp(telegramId));
    }

    /** Точечный запрос по ID — кэш не используется. */
    public PremiumTransaction getById(Integer id) {
        return premiumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Premium-транзакция не найдена: " + id));
    }

    // -------------------------------------------------------------------------
    // Вспомогательное
    // -------------------------------------------------------------------------

    private PremiumStats buildStats(List<PremiumTransaction> list) {
        int totalMonths = list.stream().mapToInt(PremiumTransaction::getMonths).sum();
        return new PremiumStats(list.size(), totalMonths);
    }

    public record PremiumStats(int count, int totalMonths) {
        public double averageMonths() {
            return count > 0 ? (double) totalMonths / count : 0.0;
        }
    }
}