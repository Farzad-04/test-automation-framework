package com.banking.controller;

import com.banking.dto.TransferRequest;
import com.banking.entity.Transaction;
import com.banking.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest request) {
        if (request == null || request.getFromAccountId() == null || request.getToAccountId() == null || request.getAmount() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Missing required transfer fields"));
        }

        try {
            Transaction transaction = transactionService.transferFunds(request);
            return ResponseEntity.ok(transactionService.toApiResponse(transaction));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            int status = HttpStatus.BAD_REQUEST.value();
            if (message != null && message.contains("not found")) {
                status = HttpStatus.NOT_FOUND.value();
            }
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", message);
            errorBody.put("message", message);
            return ResponseEntity.status(status).body(errorBody);
        }
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Map<String, Object>>> getTransactions(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccountId(accountId)
                .stream()
                .map(transactionService::toApiResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<?> getTransaction(@PathVariable Long transactionId) {
        try {
            Transaction transaction = transactionService.getTransactionById(transactionId);
            return ResponseEntity.ok(transactionService.toApiResponse(transaction));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}