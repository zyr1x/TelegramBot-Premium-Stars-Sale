package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.ActivatedReferral;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivatedReferralRepository extends JpaRepository<ActivatedReferral, Integer> {

    List<ActivatedReferral> findByReferralHash(String referralHash);

    List<ActivatedReferral> findByActivatedByUserId(Long activatedByUserId);

    boolean existsByReferralHashAndActivatedByUserId(String referralHash, Long activatedByUserId);

    Optional<ActivatedReferral> findByReferralHashAndActivatedByUserId(String referralHash, Long activatedByUserId);

    @Query("SELECT ar FROM ActivatedReferral ar " +
            "JOIN Referral r ON ar.referralHash = r.hash " +
            "WHERE r.userId = :userId " +
            "ORDER BY ar.activatedAt DESC")
    List<ActivatedReferral> findAllByReferrerUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(ar) FROM ActivatedReferral ar WHERE ar.referralHash = :hash")
    Long countByReferralHash(@Param("hash") String hash);
}
