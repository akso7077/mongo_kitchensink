// src/main/java/com/example/kitchensink/controller/AuthController.java

package com.example.kitchensink.controller;

// Corrected imports based on your code structure
import com.example.kitchensink.config.JwtTokenProvider;
import com.example.kitchensink.controller.AdminController.MessageResponse; // Using your MessageResponse location
// import com.example.kitchensink.exception.BadRequestException; // Use this if your verifyRefreshToken throws BadRequestException
import com.example.kitchensink.exception.ResourceNotFoundException;
import com.example.kitchensink.model.LoginRequest;
import com.example.kitchensink.model.SignUpRequest;
import com.example.kitchensink.model.User; // User entity is in model package
import com.example.kitchensink.model.RefreshToken; // Assuming RefreshToken entity is also in model package
import com.example.kitchensink.payload.request.RefreshTokenRequest;
import com.example.kitchensink.payload.response.JwtTokenPairResponse; // Use the updated DTO
import com.example.kitchensink.repository.UserRepository;
import com.example.kitchensink.service.UserService;

import jakarta.validation.Valid; // Correct validation import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpStatus; // Only needed if you return specific status codes manually
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
// CORRECTED IMPORT BELOW:
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // <--- Corrected import
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority; // Needed to get role names
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.*;

import java.util.List; // Needed for List
import java.util.Set; // Needed for Set
import java.util.stream.Collectors; // Needed for streams

/**
 * Controller for authentication related operations (login, signup, token refresh, logout)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // Use Lombok to generate constructor for final fields
@Slf4j // Add Slf4j for logging
// @CrossOrigin(origins = "http://localhost:8084", maxAge = 3600) // Example: Allow your frontend origin
@CrossOrigin(origins = "*", maxAge = 3600) // Allow all origins (adjust for production)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider; // Inject the modified JwtTokenProvider
    private final UserRepository userRepository; // Inject UserRepository (needed for refresh token logic and user details)
    private final UserService userService; // Assuming you handle signup here

    // Note: If your RefreshToken logic is entirely within JwtTokenProvider as shown below,
    // you might not need a separate RefreshTokenService injected here.


    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) { // Return MessageResponse DTO

        // UserService handles validation and creation
        userService.createUser(signUpRequest.getUsername(), signUpRequest.getEmail(), signUpRequest.getPassword());

        log.info("User registered successfully: {}", signUpRequest.getUsername());
        return ResponseEntity.ok(new MessageResponse("User registered successfully")); // Use MessageResponse from AdminController
    }


    @PostMapping("/login")
    public ResponseEntity<JwtTokenPairResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        // Authenticate user credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken( // This class is now correctly imported
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Set authentication in security context (optional for stateless, but good practice)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get UserDetails from the authenticated principal
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Find the full User entity from the repository to get ID and Roles
        // Assumes your UserDetailsImpl can be cast or is a User entity directly, or you find by username
        // Finding by username is safer if UserDetailsImpl is not the User entity itself
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));


        // Generate Access Token using the principal (UserDetails)
        String accessToken = tokenProvider.generateToken(authentication); // Assuming generateToken takes Authentication or UserDetails


        // Generate and Save Refresh Token using the user's ID
        // Assuming JwtTokenProvider has createRefreshToken(String userId) returning RefreshToken entity
        RefreshToken refreshTokenEntity = tokenProvider.createRefreshToken(user.getId());
        String refreshTokenString = refreshTokenEntity.getToken();


        // Get role names as a List<String> from the User entity's roles
        List<String> roles = user.getRoles().stream() // Assumes User entity has getRoles() returning Set<Role>
                // CORRECTED: Use the enum's built-in name() method to get the String name (e.g., "ROLE_USER")
                .map(role -> role.name()) // <=== THIS LINE USES THE CORRECT name() METHOD for a Role enum
                .collect(Collectors.toList());


        // Return both tokens, user ID, username, and roles in the response
        // ===> ACTION REQUIRED: Open your JwtTokenPairResponse.java.
        // ===> You MUST add a constructor that takes exactly these 5 arguments: (String accessToken, String refreshToken, String id, String username, List<String> roles)
        // ===> Your @AllArgsConstructor likely creates a 6-arg constructor (including 'type'), which doesn't match the 5 args passed here.
        return ResponseEntity.ok(new JwtTokenPairResponse(accessToken, refreshTokenString,
                user.getId(), // User ID (Argument 3 - String)
                user.getUsername(), // Username (Argument 4 - String)
                roles)); // List of role names (Argument 5 - List<String>)
        // The error message "'JwtTokenPairResponse(...)' cannot be applied to (String, String, String, String, List<String>)'"
        // means your JwtTokenPairResponse class does not have a constructor matching the 5 arguments you are passing here.
        // Add a manual constructor to JwtTokenPairResponse.java with this signature.
    }

    // Endpoint for refreshing tokens
    @PostMapping("/refresh-token")
    public ResponseEntity<JwtTokenPairResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // Verify the refresh token using the logic in JwtTokenProvider
        // Assuming verifyRefreshToken returns an Optional<RefreshToken>
        return tokenProvider.verifyRefreshToken(requestRefreshToken)
                .map(refreshToken -> {
                    // Refresh token is valid and not expired
                    User user = refreshToken.getUser(); // Get the user associated with the valid refresh token (Assuming RefreshToken entity has getUser() returning User)

                    // Generate a NEW Access Token for the user
                    // Assuming JwtTokenProvider has generateTokenFromUsername(String username)
                    String newAccessToken = tokenProvider.generateTokenFromUsername(user.getUsername());

                    // Optionally, delete the old refresh token and generate a new one after successful use
                    // This is a common pattern for enhanced security (rotate refresh tokens)
                    // tokenProvider.deleteRefreshToken(requestRefreshToken); // Assumes deleteRefreshToken takes token String
                    // RefreshToken newRefreshTokenEntity = tokenProvider.createRefreshToken(user.getId()); // Assumes createRefreshToken takes userId
                    // String newRefreshTokenString = newRefreshTokenEntity.getToken();
                    // log.info("Rotated refresh token for user: {}", user.getUsername());

                    // --- OR ---
                    // If not rotating refresh tokens, just return the same refresh token string
                    String existingRefreshTokenString = refreshToken.getToken();
                    log.info("Refreshed access token for user: {}", user.getUsername());


                    // Get role names as a List<String> from the User entity's roles
                    List<String> roles = user.getRoles().stream() // Assumes User entity has getRoles() returning Set<Role>
                            // CORRECTED: Use the enum's built-in name() method to get the String name (e.g., "ROLE_USER")
                            .map(role -> role.name()) // <=== THIS LINE USES THE CORRECT name() METHOD for a Role enum
                            .collect(Collectors.toList());

                    // Return the new access token, the (possibly new) refresh token, user ID, username, and roles
                    // Using the updated JwtTokenPairResponse constructor
                    // ===> ACTION REQUIRED: Open your JwtTokenPairResponse.java.
                    // ===> Ensure it has a constructor that takes exactly these 5 arguments: (String accessToken, String refreshToken, String id, String username, List<String> roles)
                    // ===> Your @AllArgsConstructor likely creates a 6-arg constructor (including 'type'), which doesn't match the 5 args passed here.
                    return ResponseEntity.ok(new JwtTokenPairResponse(newAccessToken, existingRefreshTokenString, // Use existing or new refresh token string
                            user.getId(), // User ID (Argument 3 - String)
                            user.getUsername(), // Username (Argument 4 - String)
                            roles)); // List of role names (Argument 5 - List<String>)
                    // The error message means your JwtTokenPairResponse class does not have a constructor matching the 5 arguments you are passing here.
                    // Add a manual constructor to JwtTokenPairResponse.java with this signature.
                })
                // If verifyRefreshToken returns empty (token not found, invalid, or expired)
                .orElseThrow(() -> {
                    log.warn("Invalid or expired refresh token received.");
                    // Use a specific exception or ResponseEntity status if preferred
                    // throw new BadRequestException("Invalid or expired refresh token");
                    return new RuntimeException("Invalid or expired refresh token"); // Re-throwing as generic RuntimeException
                });
    }

    // Logout endpoint to delete refresh tokens server-side
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logoutUser(@RequestBody(required = false) RefreshTokenRequest request) { // Accept refresh token in body

        // Client sends refresh token in the request body for logout
        if (request != null && request.getRefreshToken() != null) {
            log.info("Logout request received with refresh token.");
            // Delete the specific refresh token provided
            tokenProvider.deleteRefreshToken(request.getRefreshToken()); // Assumes deleteRefreshToken takes token String
            log.info("Provided refresh token deleted.");
            return ResponseEntity.ok(new MessageResponse("Logged out successfully."));

        } else {
            // Handle cases where no refresh token is provided in the logout request
            // This might happen if the user is just clicking logout after a long time
            log.warn("Logout request received without a refresh token in the body.");

            // Optional: Delete all tokens for the user if an access token is present (as in your original code)
             /*
             Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
             if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                 UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                 User user = userRepository.findByUsername(userDetails.getUsername())
                         .orElse(null); // Use orElse(null) or handle not found

                 if (user != null) {
                      tokenProvider.deleteRefreshTokensByUserId(user.getId()); // Assumes this method exists
                      log.info("User {} logged out, all refresh tokens deleted via access token.", user.getUsername());
                 }
             }
             */

            return ResponseEntity.ok(new MessageResponse("Logout request processed."));
        }
    }

    // You might also have other methods, e.g., for fetching user details by ID, etc.
}