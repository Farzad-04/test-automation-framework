package com.banking.service;

import com.banking.dto.TransferRequest;
import com.banking.entity.Account;
import com.banking.entity.Transaction;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TransactionService {

    private static final AtomicLong ID_SEQUENCE = new AtomicLong(30_000_000_000L);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    public Transaction transferFunds(TransferRequest request) {
        if (request == null) {
            throw new RuntimeException("Transfer request is required");
        }
        if (request.getFromAccountId() == null || request.getToAccountId() == null) {
            throw new RuntimeException("From account and to account are required");
        }
        if (request.getAmount() == null) {
            throw new RuntimeException("Amount is required");
        }
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new RuntimeException("From account not found"));

        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new RuntimeException("To account not found"));

        if (!fromAccount.getActive() || !toAccount.getActive()) {
            throw new RuntimeException("One or both accounts are inactive");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("insufficient funds");
        }

        Transaction transaction = new Transaction();
        transaction.setId(ID_SEQUENCE.getAndIncrement());
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType("TRANSFER");
        transaction.setStatus("COMPLETED");
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setCompletedDate(LocalDateTime.now());

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        fromAccount.setUpdatedAt(LocalDateTime.now());
        toAccount.setUpdatedAt(LocalDateTime.now());

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByFromAccountIdOrToAccountId(accountId, accountId);
    }

    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public Map<String, Object> toApiResponse(Transaction transaction) {
        return Map.of(
                "transactionId", transaction.getId(),
                "fromAccountId", transaction.getFromAccount() != null ? transaction.getFromAccount().getId() : null,
                "toAccountId", transaction.getToAccount() != null ? transaction.getToAccount().getId() : null,
                "amount", transaction.getAmount().floatValue(),
                "type", transaction.getTransactionType(),
                "status", transaction.getStatus(),
                "description", transaction.getDescription(),
                "transactionDate", transaction.getTransactionDate()
        );
    }
}