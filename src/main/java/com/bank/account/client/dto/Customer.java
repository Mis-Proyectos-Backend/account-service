package com.bank.account.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Customer {
    private String id;
    private String name;
    @JsonProperty("document")
    private String documentNumber;
    @JsonProperty("type")
    private String customerType;
}
