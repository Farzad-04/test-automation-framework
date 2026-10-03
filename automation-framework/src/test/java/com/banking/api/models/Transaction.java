package com.banking.api.models;

import java.time.LocalDateTime;

public class Transaction {
    public Long transactionId;
    public Long fromAccountId;
    public Long toAccountId;
    public Double amount;
    public String type;
    public String status;
    public String description;
    public LocalDateTime transactionDate;
    
    public Transaction() {}
}
