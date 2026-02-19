package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.StarsTransaction;

import java.util.List;

@Repository
public interface StarsTransactionRepository extends JpaRepository<StarsTransaction, Integer> {

    List<StarsTransaction> findByTelegramIdOrderByCreatedAtDesc(Long telegramId);

    @Query("SELECT COALESCE(SUM(s.amountStars), 0) FROM StarsTransaction s WHERE s.telegramId = :telegramId")
    long sumStarsByTelegramId(@Param("telegramId") Long telegramId);

    @Query("SELECT COUNT(s) FROM StarsTransaction s WHERE s.telegramId = :telegramId")
    long countByTelegramId(@Param("telegramId") Long telegramId);
}