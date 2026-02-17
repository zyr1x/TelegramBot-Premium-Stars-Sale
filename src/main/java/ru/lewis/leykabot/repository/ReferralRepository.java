package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.Referral;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Integer> {

    Optional<Referral> findByHash(String hash);

    Optional<Referral> findByUserId(Long userId);

    List<Referral> findAllByUserId(Long userId);

    boolean existsByHash(String hash);

    boolean existsByUserId(Long userId);

    @Query("SELECT COUNT(ar) FROM ActivatedReferral ar WHERE ar.referralHash = :hash")
    Long countActivationsByHash(@Param("hash") String hash);

    @Query("SELECT COUNT(ar) FROM ActivatedReferral ar " +
            "JOIN Referral r ON ar.referralHash = r.hash " +
            "WHERE r.userId = :userId")
    Long countActivationsByUserId(@Param("userId") Long userId);
}
