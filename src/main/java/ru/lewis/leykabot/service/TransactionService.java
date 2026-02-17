package ru.lewis.leykabot.service;

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

    /**
     * Создать транзакцию покупки звёзд
     */
    @Transactional
    public Transaction createPurchaseTransaction(Long telegramId, Integer amountRubles, Integer amountStars) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Transaction transaction = new Transaction();
        transaction.setTelegramId(telegramId);
        transaction.setAmountRubles(amountRubles);
        transaction.setAmountStars(amountStars);

        Transaction savedTransaction = transactionRepository.save(transaction);

        user.setBalance(user.getBalance() + amountRubles);
        userRepository.save(user);

        var tag = telegramService.getUsernameByUserId(telegramId);

        log.info("Создана транзакция покупки для пользователя {} {}: {} руб. -> {} звёзд",
                tag, telegramId, amountRubles, amountStars);

        telegramService.log(MessageFormat.format(logMessageConfig.getTransactionCreate(),
                tag, telegramId, amountRubles, amountStars));

        return savedTransaction;
    }

    /**
     * Получить все транзакции пользователя
     */
    public List<Transaction> getUserTransactions(Long telegramId) {
        return transactionRepository.findByTelegramIdOrderByCreatedAtDesc(telegramId);
    }

    /**
     * Получить транзакции за период
     */
    public List<Transaction> getUserTransactionsByDateRange(Long telegramId,
                                                            LocalDateTime startDate,
                                                            LocalDateTime endDate) {
        return transactionRepository.findByTelegramIdAndDateRange(telegramId, startDate, endDate);
    }

    /**
     * Получить последние N транзакций
     */
    public List<Transaction> getLastUserTransactions(Long telegramId, int limit) {
        List<Transaction> transactions = transactionRepository.findByTelegramIdOrderByCreatedAtDesc(telegramId);
        return transactions.stream().limit(limit).toList();
    }

    /**
     * Получить общую сумму купленных звёзд
     */
    public Long getTotalStarsPurchased(Long telegramId) {
        Long total = transactionRepository.getTotalStarsByTelegramId(telegramId);
        return total != null ? total : 0L;
    }

    /**
     * Получить количество транзакций пользователя
     */
    public Long getTransactionCount(Long telegramId) {
        return transactionRepository.countByTelegramId(telegramId);
    }

    /**
     * Получить статистику транзакций за последний месяц
     */
    public TransactionStats getMonthlyStats(Long telegramId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthAgo = now.minusMonths(1);

        List<Transaction> monthlyTransactions = transactionRepository
                .findByTelegramIdAndDateRange(telegramId, monthAgo, now);

        int totalTransactions = monthlyTransactions.size();
        int totalRubles = monthlyTransactions.stream()
                .mapToInt(Transaction::getAmountRubles)
                .sum();
        int totalStars = monthlyTransactions.stream()
                .mapToInt(Transaction::getAmountStars)
                .sum();

        return new TransactionStats(totalTransactions, totalRubles, totalStars);
    }

    /**
     * Получить общую статистику пользователя
     */
    public TransactionStats getAllTimeStats(Long telegramId) {
        List<Transaction> allTransactions = transactionRepository.findByTelegramId(telegramId);

        int totalTransactions = allTransactions.size();
        int totalRubles = allTransactions.stream()
                .mapToInt(Transaction::getAmountRubles)
                .sum();
        int totalStars = allTransactions.stream()
                .mapToInt(Transaction::getAmountStars)
                .sum();

        return new TransactionStats(totalTransactions, totalRubles, totalStars);
    }

    /**
     * Проверить, есть ли у пользователя транзакции
     */
    public boolean hasTransactions(Long telegramId) {
        return transactionRepository.countByTelegramId(telegramId) > 0;
    }

    /**
     * Получить транзакцию по ID
     */
    public Transaction getTransactionById(Integer id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Транзакция не найдена"));
    }

    /**
     * Статистика транзакций
     */
    public record TransactionStats(int totalTransactions, int totalRubles, int totalStars) {
        public double getAverageStarsPerTransaction() {
            return totalTransactions > 0 ? (double) totalStars / totalTransactions : 0.0;
        }

        public double getAverageRublesPerTransaction() {
            return totalTransactions > 0 ? (double) totalRubles / totalTransactions : 0.0;
        }
    }
}
