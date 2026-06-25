package com.bank.account.client.dto;

import lombok.Data;

@Data
public class Customer {
    private String id;
    private String name;
    private String documentNumber;
    private String customerType;
}
