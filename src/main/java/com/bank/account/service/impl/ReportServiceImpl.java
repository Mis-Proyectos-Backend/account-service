package com.bank.account.service.impl;

import com.bank.account.dto.ProductReportResponse;
import com.bank.account.enums.MovementType;
import com.bank.account.model.Account;
import com.bank.account.model.Movement;
import com.bank.account.repository.AccountRepository;
import com.bank.account.repository.MovementRepository;
import com.bank.account.service.MovementService;
import com.bank.account.service.ReportService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class ReportServiceImpl implements ReportService {

    private final AccountRepository accountRepository;

    private final MovementRepository movementRepository;

    public ReportServiceImpl(AccountRepository accountRepository, MovementRepository movementRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    public Mono<ProductReportResponse> generateAccountReport(
            String accountId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        Mono<Account> accountMono =
                accountRepository.findById(accountId);

        Mono<List<Movement>> movementsMono =
                movementRepository
                        .findByAccountIdAndMovementDateBetweenOrderByMovementDateDesc(
                                accountId,
                                startDate,
                                endDate)
                        .collectList();

        return Mono.zip(accountMono, movementsMono)
                .map(tuple -> {

                    Account account = tuple.getT1();
                    List<Movement> movements = tuple.getT2();

                    BigDecimal deposits =
                            movements.stream()
                                    .filter(m -> m.getMovementType() == MovementType.DEPOSIT)
                                    .map(Movement::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal withdrawals =
                            movements.stream()
                                    .filter(m -> m.getMovementType() == MovementType.WITHDRAW)
                                    .map(Movement::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return ProductReportResponse.builder()
                            .accountId(account.getId())
                            .accountNumber(account.getAccountNumber())
                            .accountType(account.getType())
                            .currentBalance(account.getBalance())
                            .totalMovements(movements.size())
                            .totalDeposits(deposits)
                            .totalWithdrawals(withdrawals)
                            .movements(movements)
                            .build();

                });

    }

    @Override
    public Flux<Movement> getLast10Movements(String accountId) {

        return movementRepository
                .findTop10ByAccountIdOrderByMovementDateDesc(accountId);

    }

}
