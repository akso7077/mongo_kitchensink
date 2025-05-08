package com.example.kitchensink.config;

import com.example.kitchensink.model.RefreshToken;
import com.example.kitchensink.model.User;
import com.example.kitchensink.repository.RefreshTokenRepository;
import com.example.kitchensink.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for JWT token generation and validation, and Refresh Token management
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private int jwtExpirationInMs;

    @Value("${app.jwt.refresh-expiration}") // Add refresh token expiration property
    private Long refreshTokenExpirationInMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;


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
            throw new RuntimeException("User not found for refresh token creation");
        }

        User user = userOptional.get();

        // This ensures that the user only has one valid refresh token at any given time.
        refreshTokenRepository.deleteByUser(user);


        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationInMs));

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
            return Optional.empty();
        }

        return Optional.of(refreshToken);
    }

    public void deleteRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    public void deleteRefreshTokensByUserId(String userId) {
        userRepository.findById(userId).ifPresent(refreshTokenRepository::deleteByUser);
    }


    public String generateTokenFromUsername(String username) {

        UserDetails userPrincipal = this.userDetailsService.loadUserByUsername(username);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        // Collect roles from authorities
        String authorities = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .claim("roles", authorities)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationInMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}