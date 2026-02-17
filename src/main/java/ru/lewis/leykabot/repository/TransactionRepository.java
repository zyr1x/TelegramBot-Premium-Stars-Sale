package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByTelegramIdOrderByCreatedAtDesc(Long telegramId);

    List<Transaction> findByTelegramId(Long telegramId);

    @Query("SELECT t FROM Transaction t WHERE t.telegramId = :telegramId " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findByTelegramIdAndDateRange(
            @Param("telegramId") Long telegramId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT SUM(t.amountStars) FROM Transaction t WHERE t.telegramId = :telegramId")
    Long getTotalStarsByTelegramId(@Param("telegramId") Long telegramId);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.telegramId = :telegramId")
    Long countByTelegramId(@Param("telegramId") Long telegramId);
}
