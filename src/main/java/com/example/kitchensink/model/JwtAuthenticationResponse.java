package com.example.kitchensink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Set;

/**
 * Response object returned after successful authentication
 * containing the JWT token
 */
@Data
@AllArgsConstructor
public class JwtAuthenticationResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String username;
    private Set<String> roles;

    public JwtAuthenticationResponse(String accessToken, String username, Set<String> roles) {
        this.accessToken = accessToken;
        this.username = username;
        this.roles = roles;
    }
}