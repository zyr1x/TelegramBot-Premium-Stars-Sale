package ru.lewis.leykabot.model.dto.fragment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StarsPurchase {
    private String reqId;
    private TransactionResponse.Transaction transaction;

}
