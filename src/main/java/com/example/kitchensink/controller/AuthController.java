package com.example.kitchensink.controller;


import com.example.kitchensink.config.JwtTokenProvider;
import com.example.kitchensink.controller.AdminController.MessageResponse;
// import com.example.kitchensink.exception.BadRequestException;
import com.example.kitchensink.exception.ResourceNotFoundException;
import com.example.kitchensink.model.LoginRequest;
import com.example.kitchensink.model.SignUpRequest;
import com.example.kitchensink.model.User;
import com.example.kitchensink.model.RefreshToken;
import com.example.kitchensink.payload.request.RefreshTokenRequest;
import com.example.kitchensink.payload.response.JwtTokenPairResponse;
import com.example.kitchensink.repository.UserRepository;
import com.example.kitchensink.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for authentication related operations (login, signup, token refresh, logout)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
// @CrossOrigin(origins = "http://localhost:8084", maxAge = 3600) // Allow  frontend origin
@CrossOrigin(origins = "*", maxAge = 3600) // Allow all origins
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final UserService userService;


    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {

        // UserService handles validation and creation
        userService.createUser(signUpRequest.getUsername(), signUpRequest.getEmail(), signUpRequest.getPassword());

        log.info("User registered successfully: {}", signUpRequest.getUsername());
        return ResponseEntity.ok(new MessageResponse("User registered successfully")); // Use MessageResponse from AdminController
    }


    @PostMapping("/login")
    public ResponseEntity<JwtTokenPairResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        // Authenticate user credentials
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get UserDetails from the authenticated principal
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Find the full User entity from the repository to get ID and Roles
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", userDetails.getUsername()));


        // Generate Access Token using the principal (UserDetails)
        String accessToken = tokenProvider.generateToken(authentication);


        // Generate and Save Refresh Token using the user's ID
        RefreshToken refreshTokenEntity = tokenProvider.createRefreshToken(user.getId());
        String refreshTokenString = refreshTokenEntity.getToken();


        // Get role names as a List<String> from the User entity's roles
        List<String> roles = user.getRoles().stream()
                .map(role -> role.name())
                .collect(Collectors.toList());


        return ResponseEntity.ok(new JwtTokenPairResponse(accessToken, refreshTokenString,
                user.getId(),
                user.getUsername(),
                roles));
    }

    // Endpoint for refreshing tokens
    @PostMapping("/refresh-token")
    public ResponseEntity<JwtTokenPairResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return tokenProvider.verifyRefreshToken(requestRefreshToken)
                .map(refreshToken -> {

                    User user = refreshToken.getUser(); // Get the user associated with the valid refresh token

                    // Generate a NEW Access Token for the user
                    String newAccessToken = tokenProvider.generateTokenFromUsername(user.getUsername());

                    String existingRefreshTokenString = refreshToken.getToken();
                    log.info("Refreshed access token for user: {}", user.getUsername());

                    List<String> roles = user.getRoles().stream()
                            .map(role -> role.name())
                            .collect(Collectors.toList());


                    return ResponseEntity.ok(new JwtTokenPairResponse(newAccessToken, existingRefreshTokenString, // Use existing or new refresh token string
                            user.getId(),
                            user.getUsername(),
                            roles));
                })
                .orElseThrow(() -> {
                    log.warn("Invalid or expired refresh token received.");
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
            tokenProvider.deleteRefreshToken(request.getRefreshToken());
            log.info("Provided refresh token deleted.");
            return ResponseEntity.ok(new MessageResponse("Logged out successfully."));

        } else {

            log.warn("Logout request received without a refresh token in the body.");
            //TODO: Can put delete here

            return ResponseEntity.ok(new MessageResponse("Logout request processed."));
        }
    }

}