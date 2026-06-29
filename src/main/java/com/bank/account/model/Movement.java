package com.bank.account.model;

import com.bank.account.enums.MovementType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "movements")
public class Movement {

    @Id
    private String id;

    private String accountId;

    private MovementType movementType;

    private BigDecimal amount;

    private BigDecimal balanceAfterMovement;

    private LocalDateTime movementDate;
}
