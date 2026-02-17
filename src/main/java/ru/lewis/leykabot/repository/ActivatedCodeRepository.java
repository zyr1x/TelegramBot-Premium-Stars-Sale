package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.ActivatedCode;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivatedCodeRepository extends JpaRepository<ActivatedCode, Integer> {

    List<ActivatedCode> findByTelegramId(Long telegramId);

    List<ActivatedCode> findByCode(String code);

    boolean existsByTelegramIdAndCode(Long telegramId, String code);

    Optional<ActivatedCode> findByTelegramIdAndCode(Long telegramId, String code);

    @Query("SELECT COUNT(ac) FROM ActivatedCode ac WHERE ac.code = :code")
    Long countByCode(@Param("code") String code);

    @Query("SELECT COUNT(ac) FROM ActivatedCode ac WHERE ac.telegramId = :telegramId")
    Long countByTelegramId(@Param("telegramId") Long telegramId);

    @Query("SELECT ac FROM ActivatedCode ac WHERE ac.telegramId = :telegramId " +
            "ORDER BY ac.activatedAt DESC")
    List<ActivatedCode> findByTelegramIdOrderByActivatedAtDesc(@Param("telegramId") Long telegramId);
}
