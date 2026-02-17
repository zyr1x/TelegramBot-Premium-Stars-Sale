package ru.lewis.leykabot.model.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activatedReferral", indexes = {
        @Index(name = "idx_referral_hash", columnList = "referralHash"),
        @Index(name = "idx_activated_by_user_id", columnList = "activatedByUserId"),
        @Index(name = "idx_activated_referral_unique_activation", columnList = "referralHash,activatedByUserId", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivatedReferral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "referralHash", nullable = false, length = 255)
    private String referralHash;

    @Column(name = "activatedByUserId", nullable = false)
    private Long activatedByUserId;

    @CreationTimestamp
    @Column(name = "activatedAt", nullable = false, updatable = false)
    private LocalDateTime activatedAt;

    // Связи
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referralHash", referencedColumnName = "hash",
            insertable = false, updatable = false)
    private Referral referral;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activatedByUserId", referencedColumnName = "telegramId",
            insertable = false, updatable = false)
    private User activatedByUser;
}