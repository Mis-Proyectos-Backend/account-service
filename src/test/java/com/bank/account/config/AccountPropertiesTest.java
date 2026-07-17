package com.bank.account.config;

import com.bank.account.enums.AccountType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AccountPropertiesTest {

    @Test
    void getByType_withSavingsType_shouldReturnSavingsConfig() {
        AccountProperties props = new AccountProperties();
        AccountProperties.AccountConfig savingsConfig = new AccountProperties.AccountConfig();
        savingsConfig.setFreeTransactions(5);
        savingsConfig.setTransactionCommission(BigDecimal.valueOf(2));
        props.setSavings(savingsConfig);

        AccountProperties.AccountConfig result = props.getByType(AccountType.SAVINGS);
        assertNotNull(result);
        assertEquals(5, result.getFreeTransactions());
        assertEquals(0, result.getTransactionCommission().compareTo(BigDecimal.valueOf(2)));
    }

    @Test
    void getByType_withCheckingType_shouldReturnCheckingConfig() {
        AccountProperties props = new AccountProperties();
        AccountProperties.AccountConfig checkingConfig = new AccountProperties.AccountConfig();
        checkingConfig.setFreeTransactions(3);
        checkingConfig.setTransactionCommission(BigDecimal.valueOf(1.5));
        props.setChecking(checkingConfig);

        AccountProperties.AccountConfig result = props.getByType(AccountType.CHECKING);
        assertNotNull(result);
        assertEquals(3, result.getFreeTransactions());
        assertEquals(0, result.getTransactionCommission().compareTo(BigDecimal.valueOf(1.5)));
    }

    @Test
    void getByType_withFixedTermType_shouldReturnFixedTermConfig() {
        AccountProperties props = new AccountProperties();
        AccountProperties.AccountConfig fixedTermConfig = new AccountProperties.AccountConfig();
        fixedTermConfig.setFreeTransactions(1);
        fixedTermConfig.setTransactionCommission(BigDecimal.ZERO);
        props.setFixedTerm(fixedTermConfig);

        AccountProperties.AccountConfig result = props.getByType(AccountType.FIXED_TERM);
        assertNotNull(result);
        assertEquals(1, result.getFreeTransactions());
        assertEquals(0, result.getTransactionCommission().compareTo(BigDecimal.ZERO));
    }

    @Test
    void getByType_withNullType_shouldThrowException() {
        AccountProperties props = new AccountProperties();

        assertThrows(IllegalArgumentException.class, () -> props.getByType(null));
    }

    @Test
    void accountConfig_settersAndGetters() {
        AccountProperties.AccountConfig config = new AccountProperties.AccountConfig();
        config.setFreeTransactions(10);
        config.setTransactionCommission(BigDecimal.valueOf(5));

        assertEquals(10, config.getFreeTransactions());
        assertEquals(0, config.getTransactionCommission().compareTo(BigDecimal.valueOf(5)));
    }
}
