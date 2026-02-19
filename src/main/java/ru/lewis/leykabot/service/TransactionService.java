package ru.lewis.leykabot.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lewis.leykabot.configuration.loc.LogMessageConfig;
import ru.lewis.leykabot.model.database.entity.Transaction;
import ru.lewis.leykabot.model.database.entity.User;
import ru.lewis.leykabot.repository.TransactionRepository;
import ru.lewis.leykabot.repository.UserRepository;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository        userRepository;
    private final TelegramService       telegramService;
    private final LogMessageConfig      logMessageConfig;
    private final UserService           userService;

    // Кэш: telegramId → список рублёвых транзакций DESC по дате
    private Cache<Long, List<Transaction>> cache;

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

    private List<Transaction> warmUp(Long telegramId) {
        return cache.get(telegramId,
                id -> transactionRepository.findByTelegramIdOrderByCreatedAtDesc(id));
    }

    /** Асинхронная подгрузка кэша — вызывается при /start. */
    public CompletableFuture<List<Transaction>> preload(Long telegramId) {
        return CompletableFuture.supplyAsync(() -> warmUp(telegramId));
    }

    public List<Transaction> refreshCache(Long telegramId) {
        List<Transaction> fresh = transactionRepository.findByTelegramIdOrderByCreatedAtDesc(telegramId);
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
     * Создаёт рублёвую транзакцию и пополняет баланс пользователя.
     * Возвращает сохранённую транзакцию — её передают в StarsService / PremiumService.
     */
    @Transactional
    public Transaction create(Long telegramId, Integer amountRubles) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + telegramId));

        Transaction tx = new Transaction();
        tx.setTelegramId(telegramId);
        tx.setAmountRubles(amountRubles);
        Transaction saved = transactionRepository.save(tx);

        user.setBalance(user.getBalance() + amountRubles);
        userRepository.save(user);

        userService.invalidateUserCache(telegramId);
        invalidateCache(telegramId);

        var tag = telegramService.getUsernameByUserId(telegramId);
        log.info("Рублёвая транзакция #{} для {} {}: +{} руб.", saved.getId(), tag, telegramId, amountRubles);
        telegramService.log(MessageFormat.format(
                logMessageConfig.getTransactionCreate(), tag, telegramId, amountRubles));

        return saved;
    }

    // -------------------------------------------------------------------------
    // Чтение — всё из кэша
    // -------------------------------------------------------------------------

    public List<Transaction> getAll(Long telegramId) {
        return warmUp(telegramId);
    }

    public List<Transaction> getLast(Long telegramId, int limit) {
        return warmUp(telegramId).stream().limit(limit).toList();
    }

    public List<Transaction> getByDateRange(Long telegramId, LocalDateTime from, LocalDateTime to) {
        return warmUp(telegramId).stream()
                .filter(t -> !t.getCreatedAt().isBefore(from) && !t.getCreatedAt().isAfter(to))
                .toList();
    }

    public long getTotalRubles(Long telegramId) {
        return warmUp(telegramId).stream().mapToLong(Transaction::getAmountRubles).sum();
    }

    public long getCount(Long telegramId) {
        return warmUp(telegramId).size();
    }

    public boolean hasAny(Long telegramId) {
        return !warmUp(telegramId).isEmpty();
    }

    public TransactionStats getMonthlyStats(Long telegramId) {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        return buildStats(warmUp(telegramId).stream()
                .filter(t -> t.getCreatedAt().isAfter(monthAgo))
                .toList());
    }

    public TransactionStats getAllTimeStats(Long telegramId) {
        return buildStats(warmUp(telegramId));
    }

    /** Точечный запрос по ID — кэш не используется. */
    public Transaction getById(Integer id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Транзакция не найдена: " + id));
    }

    // -------------------------------------------------------------------------
    // Вспомогательное
    // -------------------------------------------------------------------------

    private TransactionStats buildStats(List<Transaction> list) {
        int totalRubles = list.stream().mapToInt(Transaction::getAmountRubles).sum();
        return new TransactionStats(list.size(), totalRubles);
    }

    public record TransactionStats(int count, int totalRubles) {
        public double averageRubles() {
            return count > 0 ? (double) totalRubles / count : 0.0;
        }
    }
}