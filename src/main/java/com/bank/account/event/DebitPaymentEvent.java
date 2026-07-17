package com.bank.account.event;

import com.bank.account.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitPaymentEvent {

    private String cardNumber;

    private String accountId;

    private BigDecimal amount;

    private String description;

    private LocalDateTime date;

    private PaymentMethod paymentMethod;

}
