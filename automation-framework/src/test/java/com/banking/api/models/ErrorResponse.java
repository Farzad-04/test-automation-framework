package com.banking.api.models;

import java.time.LocalDateTime;

public class ErrorResponse {
    public int status;
    public String message;
    public String error;
    public LocalDateTime timestamp;
    public String path;
    
    public ErrorResponse() {}
}
