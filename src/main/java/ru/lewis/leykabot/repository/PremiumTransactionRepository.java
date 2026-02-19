package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.PremiumTransaction;

import java.util.List;

@Repository
public interface PremiumTransactionRepository extends JpaRepository<PremiumTransaction, Integer> {

    List<PremiumTransaction> findByTelegramIdOrderByCreatedAtDesc(Long telegramId);

    @Query("SELECT COALESCE(SUM(p.months), 0) FROM PremiumTransaction p WHERE p.telegramId = :telegramId")
    long sumMonthsByTelegramId(@Param("telegramId") Long telegramId);

    @Query("SELECT COUNT(p) FROM PremiumTransaction p WHERE p.telegramId = :telegramId")
    long countByTelegramId(@Param("telegramId") Long telegramId);
}