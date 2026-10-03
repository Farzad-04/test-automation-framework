package com.banking.controller;

import com.banking.dto.AccountDTO;
import com.banking.entity.Account;
import com.banking.security.JwtTokenUtil;
import com.banking.service.AccountService;
import com.banking.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody Map<String, Object> request) {
        Long currentUserId = JwtTokenUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        try {
            String accountType = request.get("accountType") == null ? null : request.get("accountType").toString();
            Object initialBalanceObj = request.getOrDefault("initialBalance", 0);
            BigDecimal initialBalance = new BigDecimal(initialBalanceObj.toString());
            Account account = accountService.createAccountForUser(currentUserId, accountType, initialBalance);
            return ResponseEntity.status(HttpStatus.CREATED).body(accountService.toApiResponse(account));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAccountsForCurrentUser() {
        Long currentUserId = JwtTokenUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(accountService.getAccountsForUserApi(currentUserId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountDTO>> getAccountsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<?> getAccount(@PathVariable Long accountId) {
        Long currentUserId = JwtTokenUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        try {
            Account account = accountService.getAccountForUser(currentUserId, accountId);
            return ResponseEntity.ok(accountService.toApiResponse(account));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<?> getTransactions(@PathVariable Long accountId) {
        Long currentUserId = JwtTokenUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        try {
            accountService.getAccountForUser(currentUserId, accountId);
            return ResponseEntity.ok(transactionService.getTransactionsByAccountId(accountId)
                    .stream()
                    .map(transactionService::toApiResponse)
                    .collect(Collectors.toList()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}