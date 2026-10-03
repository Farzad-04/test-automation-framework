package com.banking.api.models;

import java.time.LocalDateTime;

public class Account {
    public Long accountId;
    public Long userId;
    public String accountType;
    public String accountNumber;
    public Double balance;
    public Boolean isActive;
    public LocalDateTime createdAt;
    public LocalDateTime lastUpdated;
    
    public Account() {}
}
