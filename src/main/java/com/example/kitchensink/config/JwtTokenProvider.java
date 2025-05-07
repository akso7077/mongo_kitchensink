// src/main/java/com/example/kitchensink/config/JwtTokenProvider.java

package com.example.kitchensink.config;

import com.example.kitchensink.model.RefreshToken; // Import RefreshToken entity
import com.example.kitchensink.model.User;         // Import User entity
import com.example.kitchensink.repository.RefreshTokenRepository; // Import RefreshToken repository
import com.example.kitchensink.repository.UserRepository; // Import User repository (needed for finding user by username)
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor; // Add RequiredArgsConstructor for injecting repositories
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetailsService; // Correct import

import javax.crypto.SecretKey; // Updated import for Jakarta EE
import java.nio.charset.StandardCharsets;
import java.time.Instant; // Import Instant
import java.util.Date;
import java.util.Optional; // Import Optional
import java.util.UUID; // Import UUID for refresh token string
import java.util.stream.Collectors;

/**
 * Service responsible for JWT token generation and validation, and Refresh Token management
 */
@Component
@Slf4j
@RequiredArgsConstructor // Use Lombok to generate constructor for final fields (repositories, userDetailsService)
public class JwtTokenProvider { // Renaming to TokenProviderService might be better to reflect refresh token logic

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private int jwtExpirationInMs; // Access token expiration

    @Value("${app.jwt.refresh-expiration}") // Add refresh token expiration property
    private Long refreshTokenExpirationInMs;

    private final RefreshTokenRepository refreshTokenRepository; // Inject the new repository
    private final UserRepository userRepository; // Inject UserRepository to find user by username
    private final UserDetailsService userDetailsService; // ===> Injected UserDetailsService <===


    /**
     * Generate a JWT (Access) token for an authenticated user
     *
     * @param authentication The authentication object
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        org.springframework.security.core.userdetails.UserDetails userPrincipal =
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        // Collect roles from authorities
        String authorities = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("roles", authorities) // Store roles as a claim
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256) // Explicitly specify algorithm
                .compact();
    }

    /**
     * Extract username from JWT token
     *
     * @param token JWT token
     * @return Username
     */
    public String getUsernameFromJWT(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject(); // Get subject claim (username)
    }

    /**
     * Validate JWT token (Access Token)
     *
     * @param authToken JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String authToken) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            // Parser configured to verify signature and standard claims (like expiration)
            Jwts.parser().verifyWith(key).build().parseClaimsJws(authToken);
            return true;
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
            // Don't re-throw, just indicate invalidity
            return false;
        } catch (JwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Create and save a Refresh Token for a user
     * @param userId The ID of the user
     * @return The created RefreshToken object
     */
    public RefreshToken createRefreshToken(String userId) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            // This should ideally not happen if called after successful authentication
            throw new RuntimeException("User not found for refresh token creation");
        }

        User user = userOptional.get();

        // Clean up existing refresh tokens for the user if you want only one valid at a time
        // This ensures that the user only has one valid refresh token at any given time.
        refreshTokenRepository.deleteByUser(user);


        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString()); // Use a UUID as the token string
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationInMs)); // Set expiry

        return refreshTokenRepository.save(refreshToken); // Save to database
    }

    /**
     * Verify the Refresh Token and return the associated RefreshToken object if valid and not expired
     * @param token The refresh token string
     * @return Optional containing the RefreshToken if valid, empty otherwise
     */
    public Optional<RefreshToken> verifyRefreshToken(String token) {
        Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByToken(token);

        if (refreshTokenOptional.isEmpty()) {
            log.warn("Attempted to use non-existent refresh token: {}", token);
            return Optional.empty(); // Token not found
        }

        RefreshToken refreshToken = refreshTokenOptional.get();

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            log.warn("Attempted to use expired refresh token: {}", token);
            // Delete the expired token from DB
            refreshTokenRepository.delete(refreshToken);
            return Optional.empty(); // Token expired
        }

        // Optional: Invalidate the used refresh token immediately after verification for single-use tokens
        // This prevents token reuse. If you want multi-use until expiry, skip this deletion.
        // refreshTokenRepository.delete(refreshToken); // Delete the token after successful verification

        return Optional.of(refreshToken); // Token is valid and not expired
    }

    // You might add a method to delete a refresh token explicitly on logout later
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    public void deleteRefreshTokensByUserId(String userId) {
        userRepository.findById(userId).ifPresent(refreshTokenRepository::deleteByUser);
    }


    // ===> CORRECTED METHOD <===
    public String generateTokenFromUsername(String username) {
        // Load user details by username using the injected instance
        UserDetails userPrincipal = this.userDetailsService.loadUserByUsername(username); // <--- CORRECTED CALL

        // Build the token using user details (similar logic to your existing generateToken method)
        // This part will depend on how your existing generateToken method builds the JWT claims
        // You'll need to include userPrincipal.getUsername() and userPrincipal.getAuthorities()
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)); // Get key again here

        // Collect roles from authorities (assuming they are GrantedAuthority)
        String authorities = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // Assuming getAuthority() gives the role name string
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername())) // Set the subject to the username
                .claim("roles", authorities) // Add roles claim (make sure frontend expects comma-separated or List)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationInMs)) // Use your access token expiration
                .signWith(key, Jwts.SIG.HS256) // Use your signing key and algorithm (using Jwts.SIG as in generateToken)
                .compact();
    }
}