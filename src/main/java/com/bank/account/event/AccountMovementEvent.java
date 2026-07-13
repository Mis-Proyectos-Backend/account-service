package com.bank.account.event;

import com.bank.account.enums.AccountType;
import com.bank.account.enums.MovementType;
import com.bank.account.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountMovementEvent {

    private String accountId;

    private String accountNumber;

    private AccountType accountType;

    private String customerId;

    private MovementType movementType;

    private PaymentMethod paymentMethod;

    private BigDecimal amount;

    private BigDecimal balanceAfterMovement;

    private LocalDateTime movementDate;
}