package ru.lewis.leykabot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.lewis.leykabot.model.database.entity.PaymentEntity;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {
    List<PaymentEntity> findAllByTelegramUserId(Long telegramUserId);
}