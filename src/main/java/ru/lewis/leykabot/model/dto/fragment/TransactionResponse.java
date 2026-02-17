package ru.lewis.leykabot.model.dto.fragment;

import lombok.Data;

import java.util.List;

@Data
public class TransactionResponse {
    private boolean ok;
    private String error;
    private Transaction transaction;

    @Data
    public static class Transaction {
        private Long validUntil;
        private String from;
        private List<Message> messages;
    }

    @Data
    public static class Message {
        private String address;
        private String amount;
        private String payload;
    }
}
