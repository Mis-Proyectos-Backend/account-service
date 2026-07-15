package com.bank.account.event;

import com.bank.account.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountTransferEvent {

    private String sourceAccountId;

    private String destinationAccountId;

    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    private String description;

    private LocalDateTime date;
}