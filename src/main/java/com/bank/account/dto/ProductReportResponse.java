package com.bank.account.dto;

import com.bank.account.enums.AccountType;
import com.bank.account.model.Movement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReportResponse {

    private String accountId;

    private String accountNumber;

    private AccountType accountType;

    private BigDecimal currentBalance;

    private Integer totalMovements;

    private BigDecimal totalDeposits;

    private BigDecimal totalWithdrawals;

    private List<Movement> movements;

}
