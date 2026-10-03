package com.banking.service;

import com.banking.dto.AccountDTO;
import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final List<String> VALID_ACCOUNT_TYPES = List.of("CHECKING", "SAVINGS", "MONEY_MARKET");
    private static final AtomicLong ID_SEQUENCE = new AtomicLong(20_000_000_000L);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    public Account createAccountForUser(User user) {
        return createAccountForUser(user.getId(), user.getAccountType(), BigDecimal.ZERO);
    }

    public Account createAccountForUser(Long userId, String accountType, BigDecimal initialBalance) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String normalizedType = normalizeType(accountType);
        if (initialBalance == null) {
            initialBalance = BigDecimal.ZERO;
        }
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Initial balance cannot be negative");
        }

        Account account = new Account();
        account.setId(ID_SEQUENCE.getAndIncrement());
        account.setUser(user);
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(normalizedType);
        account.setBalance(initialBalance);
        account.setActive(true);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    public List<AccountDTO> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AccountDTO getAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return convertToDTO(account);
    }

    public Account getAccountEntityById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public Account getAccountForUser(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Account not found");
        }
        return account;
    }

    public List<Map<String, Object>> getAccountsForUserApi(Long userId) {
        return accountRepository.findByUserId(userId)
                .stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> toApiResponse(Account account) {
        return Map.of(
                "accountId", account.getId(),
                "userId", account.getUser().getId(),
                "accountType", account.getAccountType(),
                "accountNumber", account.getAccountNumber(),
                "balance", account.getBalance().floatValue(),
                "isActive", account.getActive(),
                "createdAt", account.getCreatedAt(),
                "lastUpdated", account.getUpdatedAt() != null ? account.getUpdatedAt() : account.getCreatedAt()
        );
    }

    private AccountDTO convertToDTO(Account account) {
        return new AccountDTO(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getActive()
        );
    }

    private String normalizeType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            throw new RuntimeException("Account type is required");
        }
        String normalized = accountType.trim().toUpperCase(Locale.ROOT);
        if (!VALID_ACCOUNT_TYPES.contains(normalized)) {
            throw new RuntimeException("Invalid account type");
        }
        return normalized;
    }

    private String generateAccountNumber() {
        long value = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
        return String.format("%010d", value);
    }
}
