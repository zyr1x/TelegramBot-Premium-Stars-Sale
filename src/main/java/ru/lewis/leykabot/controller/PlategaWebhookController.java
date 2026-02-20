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
            System.out.println("Webhook body: " + body);
            PaymentStatus status = PaymentStatus.valueOf((String) body.get("status"));
            String transactionId = (String) body.get("transactionId");

            if (status != PaymentStatus.CONFIRMED) return ResponseEntity.ok().build();

            plategaService.checkStatus(transactionId).thenAccept(paymentStatus -> {
                System.out.println("checkStatus result: " + paymentStatus);
                System.out.println("transactionId: " + transactionId);

                if (paymentStatus == PaymentStatus.CONFIRMED) {
                    var userId = plategaService.getUserIdByTransactionId(transactionId);
                    System.out.println("userId: " + userId);

                    if (userId != null) {
                        var amount = plategaService.getAmount(transactionId);
                        System.out.println("amount: " + amount);

                        transactionService.create(userId, amount);
                        System.out.println("transaction created");

                        var chatId = telegramService.getChatIdByUserId(userId);
                        System.out.println("chatId: " + chatId);

                        telegramService.sendMessageAuto(chatId, clientMessageConfig.getSuccessfullyCreatedTransaction());
                        System.out.println("message sent");

                        plategaService.deleteTransaction(transactionId);
                    }
                }
            }).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok().build();
        }
    }
}
