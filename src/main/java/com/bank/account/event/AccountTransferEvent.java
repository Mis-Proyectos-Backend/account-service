package com.bank.account.event;

import com.bank.account.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTransferEvent {

    private String sourceAccountId;

    private String destinationAccountId;

    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    private String description;

    private LocalDateTime date;
}