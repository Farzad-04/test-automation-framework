package com.banking.api.models;

public class TransferRequest {
    public Long fromAccountId;
    public Long toAccountId;
    public Double amount;
    public String description;
    
    public TransferRequest() {}
    
    public TransferRequest(Long fromAccountId, Long toAccountId, Double amount, String description) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.description = description;
    }
}
