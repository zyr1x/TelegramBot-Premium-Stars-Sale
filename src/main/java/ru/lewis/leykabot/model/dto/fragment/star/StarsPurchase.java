package ru.lewis.leykabot.model.dto.fragment.star;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.lewis.leykabot.model.dto.fragment.TransactionResponse;

@Data
@AllArgsConstructor
public class StarsPurchase {
    private String reqId;
    private TransactionResponse.Transaction transaction;

}
