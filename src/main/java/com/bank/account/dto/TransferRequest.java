package com.bank.account.dto;

import com.bank.account.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    private String sourceAccountId;

    private String destinationAccountId;

    private BigDecimal amount;

    private PaymentMethod paymentMethod;
}
