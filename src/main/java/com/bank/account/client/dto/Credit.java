package com.bank.account.client.dto;

import com.bank.account.enums.CreditType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Credit {

    private String id;

    private CreditType creditType;
}
