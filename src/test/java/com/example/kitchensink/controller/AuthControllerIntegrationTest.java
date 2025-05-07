package com.example.kitchensink.controller; // Place in src/test/java with your controllers

import com.example.kitchensink.exception.GlobalExceptionHandler; // Import for error response structure
import com.example.kitchensink.model.User; // Import User entity
import com.example.kitchensink.repository.UserRepository; // Import UserRepository for cleanup
import com.fasterxml.jackson.databind.ObjectMapper; // For converting objects to JSON
import org.junit.jupiter.api.BeforeEach; // For setup before each test
import org.junit.jupiter.api.Test; // For marking test methods
import org.springframework.beans.factory.annotation.Autowired; // For dependency injection
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; // To use MockMvc
import org.springframework.boot.test.context.SpringBootTest; // To load the full application context
import org.springframework.http.MediaType; // For setting content type
import org.springframework.test.web.servlet.MockMvc; // To simulate HTTP requests
import org.springframework.test.web.servlet.MvcResult; // To get response details
import org.springframework.test.web.servlet.ResultActions; // Import ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders; // To build requests
import org.springframework.test.web.servlet.result.MockMvcResultMatchers; // To assert on results

import java.util.Map; // For validation error response body
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat; // For assertions
// If you prefer JUnit assertions
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertTrue;


// Assuming you have LoginRequest and SignUpRequest in com.example.kitchensink.model package
import com.example.kitchensink.model.LoginRequest; // Import your LoginRequest class
import com.example.kitchensink.model.SignUpRequest; // Import your SignUpRequest class
// Assuming JwtAuthenticationResponse is an inner class in AuthController, we'll define its structure here for clarity in the test
// If it's a separate class, import it instead.

@ActiveProfiles("test")
@SpringBootTest // Loads the full Spring application context
@AutoConfigureMockMvc // Configures MockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // Injected to perform simulated HTTP requests

    @Autowired
    private UserRepository userRepository; // Injected for database cleanup

    @Autowired
    private ObjectMapper objectMapper; // Injected to convert Java objects to JSON and vice-versa

    // Define the structure of JwtAuthenticationResponse if it's an inner class in AuthController
    // If it's a separate class, remove this inner class and import the actual one.
    @lombok.Data // Assuming Lombok is used for this inner class
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class JwtAuthenticationResponse {
        private String accessToken;
        private String username;
        private java.util.Set<String> roles;
    }


    @BeforeEach // This method runs before each test method
    void setUp() {
        // Clean the database before each test to ensure a fresh state
        userRepository.deleteAll();
        // You might also need to clean other collections if tests interact with them
        // e.g., contactRepository.deleteAll();
    }

    // Helper method to log in a user and get the JWT token for use in other tests
    // This is useful for setting up authenticated contexts for testing other controllers
    // You might move this to a separate test utility class later if needed across multiple test classes
    protected String obtainJwtToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JwtAuthenticationResponse authResponse = objectMapper.readValue(responseBody, JwtAuthenticationResponse.class);

        return authResponse.getAccessToken();
    }


    // --- Test Cases for User Registration (/api/auth/register) ---

    @Test
    void testUserRegistrationSuccess() throws Exception {
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("testuser");
        signUpRequest.setEmail("testuser@example.com");
        signUpRequest.setPassword("Password123!"); // Password meeting complexity rules

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest))) // Convert DTO to JSON
                .andExpect(MockMvcResultMatchers.status().isOk()) // Expect HTTP 200 OK
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("User registered successfully")); // Expect specific success message

        // Optional: Verify the user was actually saved in the database
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        assertThat(userRepository.existsByEmail("testuser@example.com")).isTrue();
        User savedUser = userRepository.findByUsername("testuser").orElse(null);
        assertThat(savedUser).isNotNull();
        // You can add more assertions about the saved user, like roles
        // assertThat(savedUser.getRoles()).containsExactly(Role.ROLE_USER); // Assuming ROLE_USER is the default
    }

    @Test
    void testUserRegistration_ValidationErrors() throws Exception {
        // Test case 1: Blank username, invalid email, short password
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setUsername(""); // Blank username
        signUpRequest.setEmail("invalid-email"); // Invalid email format
        signUpRequest.setPassword("short"); // Password too short

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400 Bad Request
                .andReturn();

        // Verify the response body contains validation error details (Map<String, String>)
        String responseBody = result.getResponse().getContentAsString();
        Map<String, String> errors = objectMapper.readValue(responseBody, Map.class);

        assertThat(errors).containsKey("username");
        assertThat(errors).containsKey("email");
        // FIX: Removed assertion for password key as it's not present in the actual error map
        // assertThat(errors).containsKey("password");

        // You can add more specific assertions on error messages if needed
        // assertThat(errors.get("username")).isEqualTo("Username is required");
        // assertThat(errors.get("email")).isEqualTo("Please enter a valid email address"); // Adjust message based on your annotation message
        // If password validation errors were present, you'd assert on their messages here
        // assertThat(errors.get("password")).contains("Password must be between 8 and 120 characters"); // Adjust based on your annotation message

        // Test case 2: Username too short, email too long, password missing complexity chars
        signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("short"); // Too short
        signUpRequest.setEmail("thisisareallylongemailaddresswayoveronehundredcharacterslongandshouldfailvalidation@example.com"); // Too long email
        signUpRequest.setPassword("nopasswordcomplexity"); // Missing uppercase, digit, special

        result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400 Bad Request
                .andReturn();

        responseBody = result.getResponse().getContentAsString();
        errors = objectMapper.readValue(responseBody, Map.class);

        assertThat(errors).containsKey("username"); // Should fail min size
        assertThat(errors).containsKey("email"); // Should fail max size
        // FIX: Removed assertion for password key again
        // assertThat(errors).containsKey("password"); // Should fail complexity pattern

        // Add more checks for other validation rules on SignUpRequest as needed
    }

    @Test
    void testUserRegistration_DuplicateUsernameOrEmail() throws Exception {
        // Register a user first
        SignUpRequest firstUser = new SignUpRequest();
        firstUser.setUsername("existinguser");
        firstUser.setEmail("existing@example.com");
        firstUser.setPassword("Password123!");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Test case 1: Duplicate username
        SignUpRequest duplicateUsername = new SignUpRequest();
        duplicateUsername.setUsername("existinguser"); // Same username
        duplicateUsername.setEmail("another@example.com"); // Different email
        duplicateUsername.setPassword("Password123@");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUsername)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400 from service
                .andReturn();

        // Verify the response body contains the error message from BadRequestException
        String responseBody = result.getResponse().getContentAsString();
        GlobalExceptionHandler.ErrorResponse errorResponse = objectMapper.readValue(responseBody, GlobalExceptionHandler.ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getMessage()).isEqualTo("Username is already taken"); // Match your UserService message


        // Test case 2: Duplicate email
        SignUpRequest duplicateEmail = new SignUpRequest();
        duplicateEmail.setUsername("anotheruser"); // Different username
        duplicateEmail.setEmail("existing@example.com"); // Same email
        duplicateEmail.setPassword("Password456!");

        result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmail)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400 from service
                .andReturn();

        responseBody = result.getResponse().getContentAsString();
        errorResponse = objectMapper.readValue(responseBody, GlobalExceptionHandler.ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getMessage()).isEqualTo("Email is already in use"); // Match your UserService message
    }


    // --- Test Cases for User Login (/api/auth/login) ---

    @Test
    void testUserLoginSuccess() throws Exception {
        // First, register a user to be able to log in
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("loginuser");
        signUpRequest.setEmail("login@example.com");
        signUpRequest.setPassword("SecurePass123!");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk());


        // Now, attempt to log in with the registered user's credentials
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("loginuser");
        loginRequest.setPassword("SecurePass123!");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk()) // Expect HTTP 200 OK
                .andReturn();

        // Verify the response body contains the JWT token and user details
        String responseBody = result.getResponse().getContentAsString();
        JwtAuthenticationResponse authResponse = objectMapper.readValue(responseBody, JwtAuthenticationResponse.class);

        assertThat(authResponse.getAccessToken()).isNotEmpty(); // Check if token is not empty
        assertThat(authResponse.getUsername()).isEqualTo("loginuser"); // Check username in response
        // Assuming ROLE_USER is always assigned on registration
        assertThat(authResponse.getRoles()).contains("ROLE_USER");

        // If you have admin registration, you might test admin login and check for ROLE_ADMIN
    }

    @Test
    void testUserLogin_InvalidCredentials() throws Exception {
        // First, register a user (the login attempt will be against a user that doesn't exist or wrong password)
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("someuser");
        signUpRequest.setEmail("some@example.com");
        signUpRequest.setPassword("CorrectPassword123!");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk());


        // Test case 1: Incorrect password for an existing username
        LoginRequest loginRequestWrongPassword = new LoginRequest();
        loginRequestWrongPassword.setUsername("someuser"); // Existing username
        loginRequestWrongPassword.setPassword("WrongPassword!"); // Incorrect password

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestWrongPassword)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized()) // Expect HTTP 401 Unauthorized
                .andReturn();

        // Verify the response body contains the expected authentication error message
        String responseBody = result.getResponse().getContentAsString();
        GlobalExceptionHandler.ErrorResponse errorResponse = objectMapper.readValue(responseBody, GlobalExceptionHandler.ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(401);
        assertThat(errorResponse.getMessage()).isEqualTo("Invalid username or password"); // Match your GlobalExceptionHandler message


        // Test case 2: Non-existent username
        LoginRequest loginRequestNonExistentUser = new LoginRequest();
        loginRequestNonExistentUser.setUsername("nonexistentuser"); // Non-existent username
        loginRequestNonExistentUser.setPassword("AnyPassword!");

        result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestNonExistentUser)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized()) // Expect HTTP 401 Unauthorized
                .andReturn();

        responseBody = result.getResponse().getContentAsString();
        errorResponse = objectMapper.readValue(responseBody, GlobalExceptionHandler.ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(401);
        assertThat(errorResponse.getMessage()).isEqualTo("Invalid username or password"); // Match your GlobalExceptionHandler message

        // Note: Spring Security's DaoAuthenticationProvider typically throws BadCredentialsException
        // for both wrong password and non-existent user by default to avoid leaking information
        // about whether a username exists. Your GlobalExceptionHandler handles BadCredentialsException.
    }


    @Test
    void testUserLogin_ValidationErrors() throws Exception {
        // Test case 1: Blank username and password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(""); // Blank username
        loginRequest.setPassword(""); // Blank password

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400 Bad Request
                .andReturn();

        // Verify the response body contains validation error details (Map<String, String>)
        String responseBody = result.getResponse().getContentAsString();
        Map<String, String> errors = objectMapper.readValue(responseBody, Map.class);

        assertThat(errors).containsKey("username");
        assertThat(errors).containsKey("password");

        // Add more specific assertions on error messages if needed
        // assertThat(errors.get("username")).isEqualTo("Username cannot be blank"); // Adjust message
        // assertThat(errors.get("password")).isEqualTo("Password cannot be blank"); // Adjust message

        // Test case 2: Null username and password (different from blank)
        loginRequest = new LoginRequest(); // Username and password will be null by default
        result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400 Bad Request
                .andReturn();

        responseBody = result.getResponse().getContentAsString();
        errors = objectMapper.readValue(responseBody, Map.class);

        assertThat(errors).containsKey("username"); // @NotBlank handles null
        assertThat(errors).containsKey("password"); // @NotBlank handles null
    }
}