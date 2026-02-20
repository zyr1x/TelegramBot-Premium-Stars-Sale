package ru.lewis.leykabot.model.database.entity;

import jakarta.persistence.*;
import lombok.Data;
import ru.lewis.leykabot.model.dto.platega.PaymentStatus;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "telegram_user_id", nullable = false)
    private Long telegramUserId;

    @Column(name = "redirect")
    private String redirect;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}