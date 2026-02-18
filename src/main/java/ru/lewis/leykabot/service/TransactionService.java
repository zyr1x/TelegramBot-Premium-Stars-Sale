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

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TelegramService telegramService;
    private final LogMessageConfig logMessageConfig;
    private final UserService userService;

    // Единственный кэш — список транзакций DESC. Всё остальное считается из него.
    private Cache<Long, List<Transaction>> transactionCache;

    @PostConstruct
    public void initCaches() {
        transactionCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    // -------------------------------------------------------------------------
    // Кэш
    // -------------------------------------------------------------------------

    /** Подгружает транзакции в кэш. Если уже есть — возвращает из кэша. */
    public List<Transaction> warmUp(Long telegramId) {
        return transactionCache.get(telegramId,
                id -> transactionRepository.findByTelegramIdOrderByCreatedAtDesc(id));
    }

    /** Принудительно перечитывает из БД и обновляет кэш. */
    public List<Transaction> refreshCache(Long telegramId) {
        List<Transaction> fresh = transactionRepository.findByTelegramIdOrderByCreatedAtDesc(telegramId);
        transactionCache.put(telegramId, fresh);
        return fresh;
    }

    /** Инвалидирует кэш пользователя. */
    public void invalidateCache(Long telegramId) {
        transactionCache.invalidate(telegramId);
    }

    // -------------------------------------------------------------------------
    // Основные методы — все через кэш
    // -------------------------------------------------------------------------

    @Transactional
    public Transaction createPurchaseTransaction(Long telegramId, Integer amountRubles, Integer amountStars) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Transaction transaction = new Transaction();
        transaction.setTelegramId(telegramId);
        transaction.setAmountRubles(amountRubles);
        transaction.setAmountStars(amountStars);
        Transaction saved = transactionRepository.save(transaction);

        user.setBalance(user.getBalance() + amountRubles);
        userRepository.save(user);
        userService.refreshCache(telegramId);

        // После записи инвалидируем — следующий warmUp подхватит актуальный список
        invalidateCache(telegramId);

        var tag = telegramService.getUsernameByUserId(telegramId);
        log.info("Создана транзакция покупки для пользователя {} {}: {} руб. -> {} звёзд",
                tag, telegramId, amountRubles, amountStars);
        telegramService.log(MessageFormat.format(logMessageConfig.getTransactionCreate(),
                tag, telegramId, amountRubles, amountStars));

        return saved;
    }

    /** Все транзакции — из кэша. */
    public List<Transaction> getUserTransactions(Long telegramId) {
        return warmUp(telegramId);
    }

    /** Фильтрация по диапазону дат — из кэша, без запроса в БД. */
    public List<Transaction> getUserTransactionsByDateRange(
            Long telegramId, LocalDateTime startDate, LocalDateTime endDate) {
        return warmUp(telegramId).stream()
                .filter(t -> !t.getCreatedAt().isBefore(startDate)
                        && !t.getCreatedAt().isAfter(endDate))
                .toList();
    }

    /** Последние N транзакций — из кэша. */
    public List<Transaction> getLastUserTransactions(Long telegramId, int limit) {
        return warmUp(telegramId).stream().limit(limit).toList();
    }

    /** Сумма звёзд — из кэша. */
    public long getTotalStarsPurchased(Long telegramId) {
        return warmUp(telegramId).stream()
                .mapToLong(Transaction::getAmountStars)
                .sum();
    }

    /** Количество транзакций — из кэша. */
    public long getTransactionCount(Long telegramId) {
        return warmUp(telegramId).size();
    }

    /** Есть ли транзакции — из кэша. */
    public boolean hasTransactions(Long telegramId) {
        return !warmUp(telegramId).isEmpty();
    }

    /** Статистика за последний месяц — из кэша. */
    public TransactionStats getMonthlyStats(Long telegramId) {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        List<Transaction> list = warmUp(telegramId).stream()
                .filter(t -> t.getCreatedAt().isAfter(monthAgo))
                .toList();
        return buildStats(list);
    }

    /** Статистика за всё время — из кэша. */
    public TransactionStats getAllTimeStats(Long telegramId) {
        return buildStats(warmUp(telegramId));
    }

    /** Транзакция по ID — точечный запрос, кэш здесь не нужен. */
    public Transaction getTransactionById(Integer id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Транзакция не найдена"));
    }

    // -------------------------------------------------------------------------
    // Вспомогательное
    // -------------------------------------------------------------------------

    private TransactionStats buildStats(List<Transaction> list) {
        int totalRubles = list.stream().mapToInt(Transaction::getAmountRubles).sum();
        int totalStars  = list.stream().mapToInt(Transaction::getAmountStars).sum();
        return new TransactionStats(list.size(), totalRubles, totalStars);
    }

    public record TransactionStats(int totalTransactions, int totalRubles, int totalStars) {
        public double getAverageStarsPerTransaction() {
            return totalTransactions > 0 ? (double) totalStars / totalTransactions : 0.0;
        }
        public double getAverageRublesPerTransaction() {
            return totalTransactions > 0 ? (double) totalRubles / totalTransactions : 0.0;
        }
    }
}