package com.bank.account.model;

import com.bank.account.enums.AccountType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "accounts")
public class Account {

    @Id
    private String id;

    private String accountNumber;

    private String customerId;

    private AccountType type;

    private BigDecimal balance;

    private LocalDate createdAt;
}
