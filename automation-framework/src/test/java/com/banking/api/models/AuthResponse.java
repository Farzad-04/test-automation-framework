package com.banking.api.models;

import java.time.LocalDateTime;

public class AuthResponse {
    public Long id;
    public String firstName;
    public String lastName;
    public String email;
    public String token;
    public LocalDateTime createdAt;
    
    public AuthResponse() {}
}
