package com.banking.api.models;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import java.util.Map;

public class ValidationError {
    public int status;
    public String message;
    public String error;
    @SerializedName("fieldErrors")
    public Map<String, String> errors;
    public LocalDateTime timestamp;
    
    public ValidationError() {}
}
