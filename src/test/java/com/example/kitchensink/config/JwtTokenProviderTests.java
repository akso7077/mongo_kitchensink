package com.example.kitchensink.config; // Or your actual package

import com.example.kitchensink.model.RefreshToken; // Adjust imports
import com.example.kitchensink.model.User; // Adjust imports
import com.example.kitchensink.model.Role;
import com.example.kitchensink.repository.RefreshTokenRepository; // Adjust imports
import com.example.kitchensink.repository.UserRepository; // Adjust imports

import io.jsonwebtoken.Jwts; // Ensure you have io.jsonwebtoken dependency
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys; // For generating keys from secret

import org.junit.jupiter.api.BeforeEach; // For setup before each test
import org.junit.jupiter.api.Test; // For marking test methods
import org.mockito.InjectMocks; // To inject mocks into the test subject
import org.mockito.Mock; // To create mock objects
import org.mockito.MockitoAnnotations; // To initialize mocks
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // ===> MISSING IMPORT <===
import org.springframework.security.core.Authentication; // Import
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Import
import org.springframework.security.core.userdetails.UserDetails; // Import
import org.springframework.security.core.userdetails.UserDetailsService; // Inject this if provider loads user for generateTokenFromUsername
import org.springframework.security.core.userdetails.UsernameNotFoundException; // ===> MISSING IMPORT <===
import org.springframework.security.core.GrantedAuthority; // Import if needed for assertions

import javax.crypto.SecretKey; // Check import based on your Java version
import java.nio.charset.StandardCharsets; // Import
import java.time.Instant; // Import
import java.time.temporal.ChronoUnit; // Import
import java.util.*; // Import
import java.util.stream.Collectors; // Import

import static org.assertj.core.api.Assertions.assertThat; // ===> MISSING STATIC IMPORT <===
import static org.junit.jupiter.api.Assertions.*; // For JUnit assertions (assertThrows etc.)
import static org.mockito.ArgumentMatchers.any; // For mocking arguments
import static org.mockito.ArgumentMatchers.anyString; // For mocking String arguments
import static org.mockito.Mockito.*; // For mocking methods (when, verify, never)


// Use @SpringBootTest if JwtTokenProvider relies heavily on @Value and full Spring context
// Otherwise, a standard JUnit test with manual mock injection is sufficient.
// For simpler tests, manual injection is often preferred for better unit isolation.
class JwtTokenProviderTests {

    @InjectMocks // Inject mocks into this instance - This is the class under test
    private JwtTokenProvider jwtTokenProvider;

    @Mock // Mock dependencies of JwtTokenProvider
    private UserRepository userRepository; // If your provider interacts with User repo
    @Mock
    private RefreshTokenRepository refreshTokenRepository; // If your provider interacts with RefreshToken repo
    @Mock
    private UserDetailsService userDetailsService; // If your provider loads UserDetails (e.g., for claims)


    // Mock values for properties (if not using @SpringBootTest and @Value)
    // These should match what you configure in your application properties for tests
    private String mockJwtSecret = "thisisalongsecretkeyforjwttestingthatmeetsminimumlengthandissecure"; // Ensure sufficient length (e.g., >= 32 bytes for HS256)
    private int mockJwtExpirationInMs = 60000; // 1 minute
    private long mockRefreshTokenExpirationInMs = 86400000L; // 24 hours (Use long)

    // Add a generated SecretKey based on the mock secret for manual token creation/parsing
    private SecretKey signingKey;


    @BeforeEach // This method runs before each test method
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks

        // Generate the signing key from the mock secret
        this.signingKey = Keys.hmacShaKeyFor(mockJwtSecret.getBytes(StandardCharsets.UTF_8));


        // Manually set @Value fields if not using @SpringBootTest.
        // This is a common way to inject mock values for @Value properties in a standard JUnit test.
        // You might need reflection or adjust your provider's design if fields are final or complex.
        // Ensure the field names ("jwtSecret", "jwtExpirationInMs", "refreshTokenExpirationInMs") match
        // the private field names in your actual JwtTokenProvider class.
        try {
            java.lang.reflect.Field jwtSecretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
            jwtSecretField.setAccessible(true);
            jwtSecretField.set(jwtTokenProvider, mockJwtSecret);

            java.lang.reflect.Field jwtExpirationField = JwtTokenProvider.class.getDeclaredField("jwtExpirationInMs");
            jwtExpirationField.setAccessible(true);
            jwtExpirationField.set(jwtTokenProvider, mockJwtExpirationInMs);

            java.lang.reflect.Field refreshTokenExpirationField = JwtTokenProvider.class.getDeclaredField("refreshTokenExpirationInMs");
            refreshTokenExpirationField.setAccessible(true);
            refreshTokenExpirationField.set(jwtTokenProvider, mockRefreshTokenExpirationInMs);

            // If your provider caches the key, you might need to set that too
            // This assumes your provider has a private field named 'key' of type SecretKey
            try {
                java.lang.reflect.Field keyField = JwtTokenProvider.class.getDeclaredField("key");
                keyField.setAccessible(true);
                keyField.set(jwtTokenProvider, this.signingKey);
            } catch (NoSuchFieldException e) {
                // If the 'key' field doesn't exist, that's fine, continue.
                System.out.println("Note: No 'key' field found in JwtTokenProvider to set via reflection. Assuming key is generated on demand or in constructor.");
            }


        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Handle exceptions appropriately in a real test suite
            throw new RuntimeException("Failed to inject mock values into JwtTokenProvider via reflection", e);
        }
    }


    // --- Test Cases for Access Token Generation and Validation ---

    @Test
    void testGenerateToken_Success() {
        // Arrange: Prepare mock Authentication object
        // Create a mock UserDetails object with username and authorities (roles)
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("testuser")
                .password("encodedPassword") // Password not used in token claims, but part of UserDetails
                .authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")) // Add roles as authorities
                .build();
        // Create a mock Authentication object using the UserDetails principal
        // ===> Use the imported UsernamePasswordAuthenticationToken <===
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // Act: Generate the access token
        String token = jwtTokenProvider.generateToken(authentication);

        // Assert: Verify the token is generated and is a non-empty string
        // ===> Use the imported assertThat <===
        assertThat(token).isNotNull().isNotEmpty();

        // Optional: Further assertions by parsing the token (requires JWT parsing library)
        // This part is more complex and might make tests brittle if claims structure changes.
        // Claims claims = Jwts.parser().verifyWith(signingKey).build().parseClaimsJws(token).getBody();
        // assertThat(claims.getSubject()).isEqualTo("testuser");
        // assertThat(claims.get("roles", List.class)).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        // assertThat(claims.getIssuedAt()).isNotNull();
        // assertThat(claims.getExpiration()).isNotNull();
        // assertThat(claims.getExpiration().toInstant()).isAfter(claims.getIssuedAt().toInstant()); // Check expiry is in the future
    }

    @Test
    void testGetUsernameFromJWT_ValidToken() {
        // Arrange: Manually generate a valid token with a known subject (username)
        String testUsername = "userwithexampletoken";
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusMillis(mockJwtExpirationInMs);

        // Use the generated signing key
        String testToken = Jwts.builder()
                .setSubject(testUsername)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiration))
                .signWith(signingKey, SignatureAlgorithm.HS256) // Ensure algorithm matches your provider
                .compact();

        // Act: Extract the username from the token
        String extractedUsername = jwtTokenProvider.getUsernameFromJWT(testToken);

        // Assert: Verify the extracted username matches the one used to build the token
        // ===> Use the imported assertThat <===
        assertThat(extractedUsername).isEqualTo(testUsername);
    }

    @Test
    void testGetUsernameFromJWT_InvalidToken_ThrowsException() {
        // Arrange: Provide an invalid token string (e.g., wrong format, wrong signature)
        String invalidToken = "invalid.token.string";

        // Act & Assert: Verify that calling getUsernameFromJWT with an invalid token throws an exception
        // Depending on your JWT library and provider implementation, it might throw different exceptions (e.g., SignatureException, MalformedJwtException)
        // A general approach is to assert that *some* exception is thrown, or refine based on expected exceptions.
        assertThrows(Exception.class, () -> { // Use a more specific exception if known
            jwtTokenProvider.getUsernameFromJWT(invalidToken);
        });
    }


    @Test
    void testValidateToken_ValidToken() {
        // Arrange: Manually generate a token that is currently valid (not expired, correct signature)
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plusMillis(mockJwtExpirationInMs); // Expire in the future

        // Use the generated signing key
        String validToken = Jwts.builder()
                .setSubject("validuser")
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        // Act: Validate the token
        boolean isValid = jwtTokenProvider.validateToken(validToken);

        // Assert: Verify the token is considered valid
        // ===> Use the imported assertThat <===
        assertThat(isValid).isTrue();
    }

    @Test
    void testValidateToken_ExpiredToken() {
        // Arrange: Manually generate a token that has expired
        // Use the generated signing key
        SecretKey key = Keys.hmacShaKeyFor(mockJwtSecret.getBytes(StandardCharsets.UTF_8)); // Regenerate key for local scope if needed, or use this.signingKey

        // Ensure the expiration date is definitely in the past
        Instant issuedAt = Instant.now().minus(mockJwtExpirationInMs * 2, ChronoUnit.MILLIS); // Issued long ago
        Instant expiredAt = Instant.now().minus(1, ChronoUnit.SECONDS); // Expired 1 second ago

        String expiredToken = Jwts.builder()
                .setSubject("expireduser")
                .setIssuedAt(Date.from(issuedAt)) // Or Date.from(Instant.now().minusSeconds(60)) if expiration is 1 min
                .setExpiration(Date.from(expiredAt))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        // Act: Validate the expired token
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // Assert: Verify the token is considered invalid
        // ===> Use the imported assertThat <===
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateToken_InvalidSignature() {
        // Arrange: Manually create a token string with a valid header/payload but invalid signature
        // This requires constructing a JWT string manually with a different key or altered signature part
        String tokenParts = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjQyNjIyfQ."; // Header.Payload.
        String invalidSignature = "ThisIsAnInvalidSignatureCreatedManually123"; // A random invalid signature
        String invalidSignatureToken = tokenParts + invalidSignature;

        // Act: Validate the token with invalid signature
        boolean isValid = jwtTokenProvider.validateToken(invalidSignatureToken);

        // Assert: Verify the token is considered invalid
        // ===> Use the imported assertThat <===
        assertThat(isValid).isFalse();
    }


    // --- Test Cases for Refresh Token Management ---

    @Test
    void testCreateRefreshToken_Success() {
        // Arrange: Prepare a mock User
        User mockUser = new User(); // Assuming @NoArgsConstructor or default constructor exists
        mockUser.setId("user123");
        mockUser.setUsername("refreshuser");
        mockUser.setEmail("refresh@example.com");
        mockUser.setPassword("encodedPassword");

        // ===> Add this line to mock userRepository.findById <===
        // Tell Mockito that when userRepository.findById is called with "user123", it should return an Optional containing mockUser
        when(userRepository.findById(eq(mockUser.getId()))).thenReturn(Optional.of(mockUser));


        // Mock repository behavior: deleteByUser should do nothing, save should return the mocked entity
        // Assuming deleteByUser returns void or int, use doNothing or check return value if needed
        doNothing().when(refreshTokenRepository).deleteByUser(any(User.class));

        // Create a mock RefreshToken entity that we expect save to return
        RefreshToken mockSavedRefreshToken = new RefreshToken(); // Assuming @NoArgsConstructor or default constructor exists
        mockSavedRefreshToken.setToken(UUID.randomUUID().toString()); // Simulate token string generation
        mockSavedRefreshToken.setUser(mockUser);
        mockSavedRefreshToken.setExpiryDate(Instant.now().plusMillis(mockRefreshTokenExpirationInMs));
        mockSavedRefreshToken.setId(UUID.randomUUID().toString()); // Simulate ID generation by DB


        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mockSavedRefreshToken); // Mock the save operation


        // Act: Create a refresh token for the mock user's ID
        // Assuming createRefreshToken takes userId string
        // This call will now find the mockUser via the mocked userRepository.findById
        RefreshToken createdToken = jwtTokenProvider.createRefreshToken(mockUser.getId());

        // Assert: Verify the returned token is not null, is linked to the user, and has an expiry date
        assertThat(createdToken).isNotNull();
        assertThat(createdToken.getUser()).isEqualTo(mockUser); // Verify user link
        assertThat(createdToken.getExpiryDate()).isNotNull();
        assertThat(createdToken.getExpiryDate()).isAfter(Instant.now().minusSeconds(1)); // Verify expiry is in the near future


        // Verify repository methods were called
        verify(userRepository, times(1)).findById(eq(mockUser.getId())); // ===> Verify userRepository.findById was called <===
        verify(refreshTokenRepository, times(1)).deleteByUser(any(User.class)); // Verify old tokens were cleaned
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class)); // Verify the new token was saved
    }

    @Test
    void testVerifyRefreshToken_ValidToken() {
        // Arrange: Prepare a mock valid RefreshToken entity linked to a User
        // ===> Use no-arg constructor and setters for User and RefreshToken <===
        User mockUser = new User(); mockUser.setId("user123"); mockUser.setUsername("refreshuser");
        RefreshToken mockValidToken = new RefreshToken();
        mockValidToken.setId(UUID.randomUUID().toString());
        mockValidToken.setToken("valid-token-string");
        mockValidToken.setUser(mockUser);
        mockValidToken.setExpiryDate(Instant.now().plusSeconds(100)); // Expiring in the future


        // Mock repository behavior: findByToken should return the mock valid token
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(mockValidToken));

        // Assuming verifyRefreshToken checks for expired token and deletes it if expired
        doNothing().when(refreshTokenRepository).delete(any(RefreshToken.class)); // Mock delete


        // Act: Verify the valid refresh token
        Optional<RefreshToken> result = jwtTokenProvider.verifyRefreshToken("valid-token-string");

        // Assert: Verify the result is present and contains the mock valid token
        // ===> Use the imported assertThat <===
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("valid-token-string");
        assertThat(result.get().getUser()).isEqualTo(mockUser);

        // Verify repository methods were called
        verify(refreshTokenRepository, times(1)).findByToken(eq("valid-token-string")); // Verify findByToken was called
        // Verify delete was NOT called, as the token is valid/not expired
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    void testVerifyRefreshToken_TokenNotFound() {
        // Arrange: No token exists in the repository with the given string
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty()); // Mock findByToken to return empty

        // Act: Verify a non-existent token
        Optional<RefreshToken> result = jwtTokenProvider.verifyRefreshToken("non-existent-token-string");

        // Assert: Verify the result is empty
        // ===> Use the imported assertThat <===
        assertThat(result).isNotPresent();

        // Verify repository methods were called
        verify(refreshTokenRepository, times(1)).findByToken(eq("non-existent-token-string")); // Verify findByToken was called
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class)); // Verify delete was NOT called
    }

    @Test
    void testVerifyRefreshToken_ExpiredToken() {
        // Arrange: Prepare a mock expired RefreshToken entity
        // ===> Use no-arg constructor and setters for User and RefreshToken <===
        User mockUser = new User(); mockUser.setId("user123"); mockUser.setUsername("refreshuser");
        RefreshToken mockExpiredToken = new RefreshToken();
        mockExpiredToken.setId(UUID.randomUUID().toString());
        mockExpiredToken.setToken("expired-token-string");
        mockExpiredToken.setUser(mockUser);
        mockExpiredToken.setExpiryDate(Instant.now().minusSeconds(1)); // Expired in the past


        // Mock repository behavior: findByToken should return the mock expired token
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(mockExpiredToken));

        // Mock repository behavior: delete should do nothing when called with the expired token
        doNothing().when(refreshTokenRepository).delete(eq(mockExpiredToken));


        // Act: Verify the expired refresh token
        Optional<RefreshToken> result = jwtTokenProvider.verifyRefreshToken("expired-token-string");

        // Assert: Verify the result is empty
        // ===> Use the imported assertThat <===
        assertThat(result).isNotPresent();

        // Verify repository methods were called
        verify(refreshTokenRepository, times(1)).findByToken(eq("expired-token-string")); // Verify findByToken was called
        // Verify delete was called for the expired token
        verify(refreshTokenRepository, times(1)).delete(eq(mockExpiredToken));
    }

    @Test
    void testDeleteRefreshToken_Success() {
        // Arrange: Prepare a mock RefreshToken entity that exists
        String tokenToDelete = "token-to-remove";
        // ===> Use no-arg constructor and setters for User and RefreshToken <===
        RefreshToken mockToken = new RefreshToken();
        mockToken.setId(UUID.randomUUID().toString());
        mockToken.setToken(tokenToDelete);
        mockToken.setUser(new User()); // Link to a basic user mock
        mockToken.setExpiryDate(Instant.now());


        // Mock repository behavior: findByToken should return the mock token, delete should do nothing
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(mockToken));
        doNothing().when(refreshTokenRepository).delete(any(RefreshToken.class));


        // Act: Delete the refresh token
        jwtTokenProvider.deleteRefreshToken(tokenToDelete);

        // Assert: Verify repository methods were called
        verify(refreshTokenRepository, times(1)).findByToken(eq(tokenToDelete)); // Verify findByToken was called
        verify(refreshTokenRepository, times(1)).delete(eq(mockToken)); // Verify delete was called with the found token
    }

    @Test
    void testDeleteRefreshToken_TokenNotFound() {
        // Arrange: No token exists with the given string
        String tokenToDelete = "non-existent-token";
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty()); // Mock findByToken to return empty


        // Act: Attempt to delete a non-existent token
        jwtTokenProvider.deleteRefreshToken(tokenToDelete);

        // Assert: Verify repository methods were called, but delete was NOT called
        verify(refreshTokenRepository, times(1)).findByToken(eq(tokenToDelete)); // Verify findByToken was called
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class)); // Verify delete was NOT called
    }

    @Test
    void testDeleteRefreshTokensByUserId_Success() {
        // Arrange: Prepare a mock User entity
        String userIdToDelete = "userToDeleteTokens";
        // ===> Use no-arg constructor and setters <===
        User mockUser = new User();
        mockUser.setId(userIdToDelete);
        mockUser.setUsername("deleteuser");
        mockUser.setEmail("delete@example.com");
        mockUser.setPassword("pwd");


        // Mock repository behavior: findById should return the mock user, deleteByUser should do nothing
        when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
        doNothing().when(refreshTokenRepository).deleteByUser(any(User.class));


        // Act: Delete refresh tokens for the user ID
        jwtTokenProvider.deleteRefreshTokensByUserId(userIdToDelete);

        // Assert: Verify repository methods were called
        // ===> Use the imported assertThat <===
        assertThat(true); // Placeholder assert to avoid empty test warning if not verifying specific calls
        verify(userRepository, times(1)).findById(eq(userIdToDelete)); // Verify userRepository.findById was called
        verify(refreshTokenRepository, times(1)).deleteByUser(eq(mockUser)); // Verify refreshTokenRepository.deleteByUser was called with the found user
    }

    @Test
    void testDeleteRefreshTokensByUserId_UserNotFound() {
        // Arrange: No user exists with the given ID
        String userIdToDelete = "nonexistentUser";
        when(userRepository.findById(anyString())).thenReturn(Optional.empty()); // Mock userRepository.findById to return empty

        // Mock repository behavior: deleteByUser should do nothing if called (it shouldn't be)
        doNothing().when(refreshTokenRepository).deleteByUser(any(User.class));


        // Act: Attempt to delete refresh tokens for a non-existent user ID
        jwtTokenProvider.deleteRefreshTokensByUserId(userIdToDelete);

        // Assert: Verify userRepository.findById was called, but deleteByUser was NOT called
        // ===> Use the imported assertThat <===
        assertThat(true); // Placeholder assert
        verify(userRepository, times(1)).findById(eq(userIdToDelete)); // Verify userRepository.findById was called
        verify(refreshTokenRepository, never()).deleteByUser(any(User.class)); // Verify deleteByUser was NOT called
    }


    // --- Test Cases for generateTokenFromUsername ---

    @Test
    void testGenerateTokenFromUsername_Success() {
        // Arrange: Prepare a mock UserDetails object
        String testUsername = "userForNewToken";
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(testUsername)
                .password("encodedPassword")
                .authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_EDITOR")) // Include roles as authorities
                .build();

        // Mock UserDetailsService behavior: loadUserByUsername should return the mock UserDetails
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);

        // Act: Generate a token from the username
        String newToken = jwtTokenProvider.generateTokenFromUsername(testUsername);

        // Assert: Verify a token is generated and is not empty
        // ===> Use the imported assertThat <===
        assertThat(newToken).isNotNull().isNotEmpty();

        // Optional: Further assertions by parsing the token and checking claims (more complex)
        // Claims claims = Jwts.parser().verifyWith(signingKey).build().parseClaimsJws(newToken).getBody();
        // assertThat(claims.getSubject()).isEqualTo(testUsername);
        // List<String> roles = claims.get("roles", List.class);
        // assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_EDITOR");

        // Verify UserDetailsService was called
        verify(userDetailsService, times(1)).loadUserByUsername(eq(testUsername));
    }

    @Test
    void testGenerateTokenFromUsername_UserNotFound_ThrowsException() {
        // Arrange: Mock UserDetailsService to throw exception when user is not found
        String nonExistentUsername = "nonexistentuser";
        // ===> Use the imported UsernameNotFoundException <===
        when(userDetailsService.loadUserByUsername(anyString())).thenThrow(new UsernameNotFoundException("User not found"));

        // Act & Assert: Verify that calling generateTokenFromUsername throws UsernameNotFoundException
        // ===> Use JUnit's assertThrows <===
        assertThrows(UsernameNotFoundException.class, () -> {
            jwtTokenProvider.generateTokenFromUsername(nonExistentUsername);
        });

        // Verify UserDetailsService was called
        verify(userDetailsService, times(1)).loadUserByUsername(eq(nonExistentUsername));
    }
}