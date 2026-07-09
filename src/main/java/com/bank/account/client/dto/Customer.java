package com.bank.account.client.dto;

import com.bank.account.enums.CustomerProfile;
import com.bank.account.enums.CustomerType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Customer {

    private String id;

    private String name;

    @JsonProperty("document")
    private String documentNumber;

    @JsonProperty("type")
    private CustomerType customerType;

    @JsonProperty("customerProfile")
    private CustomerProfile customerProfile;
}
