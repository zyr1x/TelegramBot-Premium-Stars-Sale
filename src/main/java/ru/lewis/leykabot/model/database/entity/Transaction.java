package ru.lewis.leykabot.model.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Транзакция пополнения баланса в рублях.
 * Является "родительской" для StarsTransaction и PremiumTransaction.
 */
@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "idx_transaction_telegram_id", columnList = "telegramId"),
        @Index(name = "idx_transaction_created_at",  columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "telegramId", nullable = false)
    private Long telegramId;

    /** Сумма пополнения в рублях */
    @Column(name = "amount_rubles", nullable = false)
    private Integer amountRubles;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegramId", referencedColumnName = "telegramId",
            insertable = false, updatable = false)
    private User user;
}