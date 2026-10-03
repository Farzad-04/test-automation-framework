package com.banking.api.models;

import java.time.LocalDate;

public class RegisterRequest {
    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public LocalDate dateOfBirth;
    
    public RegisterRequest() {}
    
    public RegisterRequest(String firstName, String lastName, String email, String password, LocalDate dateOfBirth) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
    }
}
