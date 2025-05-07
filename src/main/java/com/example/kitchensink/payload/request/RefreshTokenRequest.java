// src/main/java/com/example/kitchensink/payload/request/RefreshTokenRequest.java
package com.example.kitchensink.payload.request;

import jakarta.validation.constraints.NotBlank; // Assuming using Bean Validation
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor; // Add AllArgsConstructor

/**
 * Request DTO for refresh token requests
 */
@Data
@AllArgsConstructor // Add AllArgsConstructor for potential testing or convenience
@NoArgsConstructor
public class RefreshTokenRequest {
    @NotBlank // Ensure the refresh token is not empty
    private String refreshToken;
}