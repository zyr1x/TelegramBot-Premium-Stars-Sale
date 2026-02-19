package ru.lewis.leykabot.model.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Транзакция покупки Telegram Stars.
 * Привязана к рублёвой транзакции {@link Transaction}.
 */
@Entity
@Table(name = "stars_transaction", indexes = {
        @Index(name = "idx_stars_telegram_id",  columnList = "telegramId"),
        @Index(name = "idx_stars_created_at",   columnList = "createdAt"),
        @Index(name = "idx_stars_transaction_id", columnList = "transaction_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StarsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "telegramId", nullable = false)
    private Long telegramId;

    /** Количество купленных звёзд */
    @Column(name = "amount_stars", nullable = false)
    private Integer amountStars;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Рублёвая транзакция, которая оплатила эту покупку.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegramId", referencedColumnName = "telegramId",
            insertable = false, updatable = false)
    private User user;
}