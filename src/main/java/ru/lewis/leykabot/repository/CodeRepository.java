package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.Code;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodeRepository extends JpaRepository<Code, Integer> {

    Optional<Code> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT c FROM Code c WHERE c.expiresAt IS NULL OR c.expiresAt > :now")
    List<Code> findAllActive(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Code c WHERE c.usedCount < c.usageLimit " +
            "AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    List<Code> findAllAvailable(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Code c SET c.usedCount = c.usedCount + 1 WHERE c.code = :code")
    void incrementUsedCount(@Param("code") String code);

    @Query("SELECT c FROM Code c WHERE c.expiresAt < :now")
    List<Code> findAllExpired(@Param("now") LocalDateTime now);
}
