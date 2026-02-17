package ru.lewis.leykabot.model.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "codes", indexes = {
        @Index(name = "idx_code", columnList = "code", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Code {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", unique = true, nullable = false, length = 255)
    private String code;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "usageLimit", nullable = false)
    private Integer usageLimit = 1;

    @Column(name = "usedCount", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "expiresAt")
    private LocalDateTime expiresAt;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Связи
    @OneToMany(mappedBy = "codeEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivatedCode> activations = new ArrayList<>();

    // Методы для проверки
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canBeUsed() {
        return !isExpired() && usedCount < usageLimit;
    }
}