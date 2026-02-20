package ru.lewis.leykabot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.lewis.leykabot.configuration.loc.ClientMessageConfig;
import ru.lewis.leykabot.model.dto.platega.PaymentStatus;
import ru.lewis.leykabot.service.PlategaService;
import ru.lewis.leykabot.service.TelegramService;
import ru.lewis.leykabot.service.TransactionService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PlategaWebhookController {
    private final TelegramService telegramService;
    private final PlategaService plategaService;
    private final TransactionService transactionService;
    private final ClientMessageConfig clientMessageConfig;

    @PostMapping("/webhook/payment")
    public ResponseEntity<Void> handlePaymentWebhook(
            @RequestHeader("X-MerchantId") String merchantId,
            @RequestHeader("X-Secret") String secret,
            @RequestBody Map<String, Object> body) {
        try {
            PaymentStatus status = PaymentStatus.valueOf((String) body.get("status"));
            String transactionId = (String) body.get("transactionId");
            double amount = ((Number) body.get("amount")).doubleValue();

            if (status != PaymentStatus.CONFIRMED) return ResponseEntity.ok().build();

            plategaService.checkStatus(transactionId).thenAccept(paymentStatus -> {
                if (paymentStatus == PaymentStatus.CONFIRMED) {
                    var userId = plategaService.getUserIdByTransactionId(transactionId);
                    if (userId != null) {
                        transactionService.create(userId, (int) amount);
                        var chatId = telegramService.getChatIdByUserId(userId);
                        telegramService.sendMessageAuto(chatId, clientMessageConfig.getSuccessfullyCreatedTransaction());
                    }
                }
            });

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok().build();
        }
    }
}
