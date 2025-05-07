// src/main/java/com/example/kitchensink/payload/response/JwtTokenPairResponse.java
package com.example.kitchensink.payload.response;

import lombok.AllArgsConstructor; // Add Lombok annotations if you use them
import lombok.Getter;
import lombok.Setter;
// import lombok.NoArgsConstructor; // Optional, add if you need a no-arg constructor

import java.util.List;

@Getter // Lombok getter
@Setter // Lombok setter
// You can keep @AllArgsConstructor if you need the 6-argument constructor elsewhere,
// but you MUST add the specific 5-argument constructor below for AuthController.
@AllArgsConstructor // <--- You can keep this if needed, but it's the source of the 6-arg mismatch

public class JwtTokenPairResponse {
    private String accessToken;
    private String refreshToken;
    private String type = "Bearer"; // Default value

    private String id; // Add this field
    private String username; // Add this field
    private List<String> roles; // Add this field (List of role names)

    // ===> ADD THIS MANUAL CONSTRUCTOR <===
    // This constructor matches the 5 arguments passed from AuthController
    public JwtTokenPairResponse(String accessToken, String refreshToken, String id, String username, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        // The 'type' field will get its default value "Bearer" automatically
        this.id = id;
        this.username = username;
        this.roles = roles;
    }

    // If not using Lombok, add a constructor and getters/setters manually for ALL fields.
    // The above manual constructor is necessary to fix the error you are seeing.
}