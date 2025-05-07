package com.example.kitchensink.model;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration requests
 */
@Data
public class SignUpRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 8, max = 50, message = "Username must be between 8 and 30 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 120, message = "Password must be between 8 and 120 characters")
    private String password;
}
