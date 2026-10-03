package com.banking.utils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class TestDataBuilder {

    public static Map<String, Object> buildUserRegistrationRequest(String email, String password) {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);
        request.put("firstName", "Test");
        request.put("lastName", "User");
        request.put("phoneNumber", "647-555-1234");
        request.put("accountType", "CHECKING");
        return request;
    }

    public static Map<String, Object> buildLoginRequest(String email, String password) {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);
        return request;
    }

    public static Map<String, Object> buildTransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("fromAccountId", fromAccountId);
        request.put("toAccountId", toAccountId);
        request.put("amount", amount);
        request.put("description", "Test transfer");
        return request;
    }

    public static String generateUniqueEmail() {
        return "user_" + System.currentTimeMillis() + "@test.com";
    }
}