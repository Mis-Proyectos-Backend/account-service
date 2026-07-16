package com.bank.account.config;

import com.bank.account.enums.AccountType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bank.account")
public class AccountProperties {

    private AccountConfig savings = new AccountConfig();

    private AccountConfig checking = new AccountConfig();

    private AccountConfig fixedTerm = new AccountConfig();

    @Getter
    @Setter
    public static class AccountConfig {

        private Integer freeTransactions;

        private BigDecimal transactionCommission;

        private Integer movementDay;

    }

    public AccountConfig getByType(AccountType type) {

        if (type == null) {
            throw new IllegalArgumentException("El tipo de cuenta no puede ser nulo");
        }

        return switch (type) {
            case SAVINGS -> savings;
            case CHECKING -> checking;
            case FIXED_TERM -> fixedTerm;
        };
    }
}
