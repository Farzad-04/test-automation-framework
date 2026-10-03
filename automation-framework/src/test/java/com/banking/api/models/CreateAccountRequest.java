package com.banking.api.models;

public class CreateAccountRequest {
    public String accountType;
    public Double initialBalance;
    
    public CreateAccountRequest() {}
    
    public CreateAccountRequest(String accountType, Double initialBalance) {
        this.accountType = accountType;
        this.initialBalance = initialBalance;
    }
}
