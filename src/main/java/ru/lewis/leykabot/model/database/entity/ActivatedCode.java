package ru.lewis.leykabot.model.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activatedCodes", indexes = {
        @Index(name = "idx_active_codes_telegram_id", columnList = "telegramId"),
        @Index(name = "idx_activated_codes_code", columnList = "code"),
        @Index(name = "idx_unique_activation", columnList = "telegramId,code", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivatedCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "telegramId", nullable = false)
    private Long telegramId;

    @Column(name = "code", nullable = false, length = 255)
    private String code;

    @CreationTimestamp
    @Column(name = "activatedAt", nullable = false, updatable = false)
    private LocalDateTime activatedAt;

    // Связи
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegramId", referencedColumnName = "telegramId",
            insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code", referencedColumnName = "code",
            insertable = false, updatable = false)
    private Code codeEntity;
}
