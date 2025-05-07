package com.example.kitchensink.service; // Place in src/test/java with your services

import com.example.kitchensink.exception.BadRequestException; // Import your custom exceptions
import com.example.kitchensink.exception.ResourceNotFoundException;
import com.example.kitchensink.model.LoginRequest; // Import DTOs (not used in this test class anymore)
import com.example.kitchensink.model.Role; // Import enums/entities
import com.example.kitchensink.model.SignUpRequest; // Still needed for creation tests
import com.example.kitchensink.model.User;
import com.example.kitchensink.repository.UserRepository; // Import repositories

// FIX: Removed imports related to JWT and Spring Security AuthenticationManager/UserDetailsImpl
// import com.example.kitchensink.config.JwtTokenProvider;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;


import org.junit.jupiter.api.BeforeEach; // JUnit 5 annotations
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks; // Mockito annotations
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
// FIX: Removed unused Spring Security imports
// import org.springframework.security.authentication.BadCredentialsException;


import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections; // Utility collections
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
// FIX: Removed unused stream imports
// import java.util.stream.Collectors;


import static org.assertj.core.api.Assertions.assertThat; // AssertJ assertions
import static org.junit.jupiter.api.Assertions.assertThrows; // JUnit exception assertion
import static org.mockito.ArgumentMatchers.any; // Mockito argument matchers
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient; // For 'when' that might not be called in all paths
import static org.mockito.Mockito.never; // Mockito verification
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class) // Integrates Mockito with JUnit 5
class UserServiceTest {

    @Mock // Creates a mock object of UserRepository
    private UserRepository userRepository;

    @Mock // Creates a mock object of PasswordEncoder
    private PasswordEncoder passwordEncoder;

    // FIX: Removed mocks for AuthenticationManager and JwtTokenProvider
    // @Mock
    // private AuthenticationManager authenticationManager;
    // @Mock
    // private JwtTokenProvider tokenProvider;


    @InjectMocks // Injects the mock objects into this UserService instance
    private UserService userService;

    private User testUser;
    private User adminUser;
    private SignUpRequest signUpRequest;
    // FIX: Removed LoginRequest as it's not used in this test class anymore
    // private LoginRequest loginRequest;

    // FIX: Removed JwtAuthenticationResponse inner class as it's only relevant for login response
    // @lombok.Data
    // @lombok.NoArgsConstructor
    // @lombok.AllArgsConstructor
    // public static class JwtAuthenticationResponse {
    //     private String accessToken;
    //     private String username;
    //     private java.util.Set<String> roles;
    // }


    @BeforeEach // Runs before each test method
    void setUp() {
        // Initialize common objects before each test

        // Sample User objects (representing data from the database)
        Set<Role> testUserRoles = new HashSet<>();
        testUserRoles.add(Role.ROLE_USER);
        testUser = new User("user1", "user1@example.com", "encodedpassword");
        testUser.setId("user1id"); // ID is present in the User entity
        testUser.setRoles(testUserRoles);


        Set<Role> adminUserRoles = new HashSet<>();
        adminUserRoles.add(Role.ROLE_USER);
        adminUserRoles.add(Role.ROLE_ADMIN);
        adminUser = new User("adminuser", "admin@example.com", "encodedadminpassword");
        adminUser.setId("adminuserid"); // ID is present in the User entity
        adminUser.setRoles(adminUserRoles);


        // Sample DTOs (SignUpRequest is still needed for creation tests)
        signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("newuser");
        signUpRequest.setEmail("newuser@example.com");
        signUpRequest.setPassword("Password123!");


        // FIX: Removed LoginRequest initialization
        // loginRequest = new LoginRequest();
        // loginRequest.setUsername("user1");
        // loginRequest.setPassword("rawpassword");
    }

    // --- Tests for User Registration (createUser) ---

    // FIX: Corrected test method name
    @Test
    void testCreateUser_Success() {
        // Arrange
        when(userRepository.existsByUsername(signUpRequest.getUsername())).thenReturn(false); // Mock repo to indicate username is not taken
        when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(false);     // Mock repo to indicate email is not taken
        when(passwordEncoder.encode(signUpRequest.getPassword())).thenReturn("encodedpassword"); // Mock password encoding
        // Mock saving the user - return a User object that resembles what the repo would save
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            // Simulate DB assigning an ID and returning the user
            // FIX: Assuming User has a constructor that takes username, email, password
            User savedUser = new User(user.getUsername(), user.getEmail(), user.getPassword());
            savedUser.setId("generated-id");
            savedUser.setRoles(user.getRoles());
            return savedUser;
        });

        // Act
        // FIX: Call createUser with individual string arguments
        User createdUser = userService.createUser(signUpRequest.getUsername(), signUpRequest.getEmail(), signUpRequest.getPassword());

        // Assert
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isNotEmpty(); // Verify ID was set (by mock)
        assertThat(createdUser.getUsername()).isEqualTo(signUpRequest.getUsername());
        assertThat(createdUser.getEmail()).isEqualTo(signUpRequest.getEmail());
        // Cannot assert encoded password directly, but can verify encoding happened
        assertThat(createdUser.getRoles()).containsExactly(Role.ROLE_USER); // Verify default role is assigned

        // Verify repository and encoder methods were called
        verify(userRepository, times(1)).existsByUsername(signUpRequest.getUsername());
        verify(userRepository, times(1)).existsByEmail(signUpRequest.getEmail());
        verify(passwordEncoder, times(1)).encode(signUpRequest.getPassword());
        verify(userRepository, times(1)).save(any(User.class)); // Verify save was called with any User object
    }

    // FIX: Corrected test method name
    @Test
    void testCreateUser_DuplicateUsername_ThrowsBadRequestException() {
        // Arrange
        when(userRepository.existsByUsername(signUpRequest.getUsername())).thenReturn(true); // Mock repo to indicate username is taken

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            // FIX: Call createUser with individual string arguments
            userService.createUser(signUpRequest.getUsername(), signUpRequest.getEmail(), signUpRequest.getPassword());
        });

        assertThat(exception.getMessage()).isEqualTo("Username is already taken"); // Verify the exception message

        // Verify only the username check was performed
        verify(userRepository, times(1)).existsByUsername(signUpRequest.getUsername());
        verify(userRepository, never()).existsByEmail(anyString());     // Email check should not be performed
        verify(passwordEncoder, never()).encode(anyString());           // Encoding should not happen
        verify(userRepository, never()).save(any(User.class));         // Save should not happen
    }

    // FIX: Corrected test method name
    @Test
    void testCreateUser_DuplicateEmail_ThrowsBadRequestException() {
        // Arrange
        when(userRepository.existsByUsername(signUpRequest.getUsername())).thenReturn(false); // Mock username is not taken
        when(userRepository.existsByEmail(signUpRequest.getEmail())).thenReturn(true);     // Mock repo to indicate email is taken

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            // FIX: Call createUser with individual string arguments
            userService.createUser(signUpRequest.getUsername(), signUpRequest.getEmail(), signUpRequest.getPassword());
        });

        assertThat(exception.getMessage()).isEqualTo("Email is already in use"); // Verify the exception message

        // Verify username check and email check were performed, but not encoding/save
        verify(userRepository, times(1)).existsByUsername(signUpRequest.getUsername());
        verify(userRepository, times(1)).existsByEmail(signUpRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Tests for Admin User Creation (createAdminUser) ---
    @Test
    void testCreateAdminUser_Success() {
        // Arrange
        String adminUsername = "newadmin";
        String adminEmail = "newadmin@example.com";
        String adminPassword = "AdminPassword123!";

        when(userRepository.existsByUsername(adminUsername)).thenReturn(false); // Mock repo to indicate username is not taken
        when(userRepository.existsByEmail(adminEmail)).thenReturn(false);     // Mock repo to indicate email is not taken
        when(passwordEncoder.encode(adminPassword)).thenReturn("encodedadminpassword"); // Mock password encoding
        // Mock saving the user - return a User object that resembles what the repo would save
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            // Simulate DB assigning an ID and returning the user
            // FIX: Assuming User has a constructor that takes username, email, password
            User savedUser = new User(user.getUsername(), user.getEmail(), user.getPassword());
            savedUser.setId("generated-id");
            savedUser.setRoles(user.getRoles());
            return savedUser;
        });

        // Act
        User createdAdminUser = userService.createAdminUser(adminUsername, adminEmail, adminPassword);

        // Assert
        assertThat(createdAdminUser).isNotNull();
        assertThat(createdAdminUser.getId()).isNotEmpty(); // Verify ID was set (by mock)
        assertThat(createdAdminUser.getUsername()).isEqualTo(adminUsername);
        assertThat(createdAdminUser.getEmail()).isEqualTo(adminEmail);
        // Cannot assert encoded password directly, but can verify encoding happened
        assertThat(createdAdminUser.getRoles()).containsExactlyInAnyOrder(Role.ROLE_USER, Role.ROLE_ADMIN); // Verify admin roles are assigned

        // Verify repository and encoder methods were called
        verify(userRepository, times(1)).existsByUsername(adminUsername);
        verify(userRepository, times(1)).existsByEmail(adminEmail);
        verify(passwordEncoder, times(1)).encode(adminPassword);
        verify(userRepository, times(1)).save(any(User.class)); // Verify save was called with any User object
    }

    @Test
    void testCreateAdminUser_DuplicateUsername_ThrowsBadRequestException() {
        // Arrange
        String adminUsername = "existingadmin";
        String adminEmail = "newadmin@example.com";
        String adminPassword = "AdminPassword123!";

        when(userRepository.existsByUsername(adminUsername)).thenReturn(true); // Mock username is taken

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            userService.createAdminUser(adminUsername, adminEmail, adminPassword);
        });

        assertThat(exception.getMessage()).isEqualTo("Username is already taken"); // Verify message

        // Verify only username check was called
        verify(userRepository, times(1)).existsByUsername(adminUsername);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateAdminUser_DuplicateEmail_ThrowsBadRequestException() {
        // Arrange
        String adminUsername = "newadmin";
        String adminEmail = "existingadmin@example.com";
        String adminPassword = "AdminPassword123!";

        when(userRepository.existsByUsername(adminUsername)).thenReturn(false); // Username unique
        when(userRepository.existsByEmail(adminEmail)).thenReturn(true);     // Mock email is taken

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            userService.createAdminUser(adminUsername, adminEmail, adminPassword);
        });

        assertThat(exception.getMessage()).isEqualTo("Email is already in use"); // Verify message

        // Verify checks were called
        verify(userRepository, times(1)).existsByUsername(adminUsername);
        verify(userRepository, times(1)).existsByEmail(adminEmail);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }


    // --- Removed Login Tests ---
    // The login logic is not in this UserService, so these tests are removed.
    // @Test
    // void testLogin_Success() { ... }
    // @Test
    // void testLogin_InvalidCredentials_ThrowsAuthenticationException() { ... }


    // --- Tests for User Retrieval and Admin User Management ---

    @Test
    void testGetAllUsers_Success() {
        // Arrange
        List<User> userList = List.of(testUser, adminUser); // Create a list of sample users
        when(userRepository.findAll()).thenReturn(userList); // Mock repo to return the list

        // Act
        List<User> retrievedUsers = userService.getAllUsers();

        // Assert
        assertThat(retrievedUsers).hasSize(2);
        // Comparing list contents directly using AssertJ's containsExactlyInAnyOrder
        assertThat(retrievedUsers).containsExactlyInAnyOrder(userList.toArray(new User[0]));

        // Verify repo method was called
        verify(userRepository, times(1)).findAll();
    }

    // Note: getUserByUsername exists in your actual service, but wasn't tested before. Let's add a test for it.
    @Test
    void testGetUserByUsername_Success() {
        // Arrange
        String username = "findme";
        User userToFind = new User(username, "findme@example.com", "encoded");
        userToFind.setId("someid");
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userToFind)); // Mock repo to return the user

        // Act
        User foundUser = userService.getUserByUsername(username);

        // Assert
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo(username);

        // Verify repo method was called
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void testGetUserByUsername_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        String username = "nonexistentusername";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty()); // Mock repo to return empty

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserByUsername(username);
        });

        // FIX: Updated assertion message to match actual service exception format
        assertThat(exception.getMessage()).isEqualTo("User not found with username : '" + username + "'"); // Verify exception message

        // Verify repo method was called
        verify(userRepository, times(1)).findByUsername(username);
    }


    @Test
    void testGetUserById_Success() {
        // Arrange
        String userId = "someuserid";
        User userToFind = new User("findme", "findme@example.com", "encoded");
        userToFind.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userToFind)); // Mock repo to return the user

        // Act
        User foundUser = userService.getUserById(userId);

        // Assert
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getId()).isEqualTo(userId);
        assertThat(foundUser.getUsername()).isEqualTo("findme");

        // Verify repo method was called
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void testGetUserById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        String userId = "nonexistentid";
        when(userRepository.findById(userId)).thenReturn(Optional.empty()); // Mock repo to return empty

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(userId);
        });

        // FIX: Updated assertion message to match actual service exception format
        assertThat(exception.getMessage()).isEqualTo("User not found with id : '" + userId + "'"); // Verify exception message

        // Verify repo method was called
        verify(userRepository, times(1)).findById(userId);
    }


    @Test
    void testUpdateUser_Success() {
        // Arrange
        String userId = "user1id"; // ID of the user to update (testUser in this case)
        User existingUser = testUser; // Use testUser as the existing user

        String updatedUsername = "user1_updated";
        String updatedEmail = "user1_updated@example.com";
        String updatedPassword = "NewPassword123!";
        Set<Role> updatedRoles = new HashSet<>();
        updatedRoles.add(Role.ROLE_USER);
        updatedRoles.add(Role.ROLE_ADMIN); // Promote to admin

        // Mock finding the existing user
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // Mock uniqueness checks (should return false as we are updating existing user to unique values)
        // Using lenient because these mocks might not be called if the input values are null or unchanged
        lenient().when(userRepository.existsByUsernameAndIdNot(anyString(), anyString())).thenReturn(false);
        lenient().when(userRepository.existsByEmailAndIdNot(anyString(), anyString())).thenReturn(false);


        // Mock password encoding if password is not null and different
        when(passwordEncoder.encode(updatedPassword)).thenReturn("encodednewpassword");

        // Mock the save operation to return the modified user object
        // In a unit test, you typically rely on Mockito's default behavior or use thenAnswer
        // Let's use thenAnswer to simulate the service setting fields and returning the updated object
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            // Simulate the service setting the fields (these should be set by the service code being tested)
            // We apply changes to a *new* user object or the argument to return it clean,
            // or apply to existingUser if the service modifies the fetched entity directly.
            // Let's simulate applying to the argument passed to save
            // FIX: Assuming User has a constructor that takes username, email, password
            User savedUser = new User(userToSave.getUsername(), userToSave.getEmail(), userToSave.getPassword());
            savedUser.setId(userToSave.getId());
            savedUser.setRoles(userToSave.getRoles());
            return savedUser; // Return the simulated saved user
        });


        // Act
        User updatedUser = userService.updateUser(userId, updatedUsername, updatedEmail, updatedPassword, updatedRoles);

        // Assert
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getId()).isEqualTo(userId); // Verify ID is retained
        // FIX: The assertion here is correct based on intended functionality, but the service
        // needs to be updated to actually set the username.
        // Assuming the service *will be* updated, this assertion remains correct for the test goal.
        assertThat(updatedUser.getUsername()).isEqualTo(updatedUsername); // Verify username update
        assertThat(updatedUser.getEmail()).isEqualTo(updatedEmail);     // Verify email update
        // Cannot assert encoded password directly from the returned object unless service exposes it (bad practice)
        // The verification below checks if encode was called.
        assertThat(updatedUser.getRoles()).containsExactlyInAnyOrder(Role.ROLE_USER, Role.ROLE_ADMIN); // Verify role update

        // Verify methods were called
        verify(userRepository, times(1)).findById(userId);
        // Verify uniqueness checks were called if the values were provided and different
        if (updatedUsername != null && !existingUser.getUsername().equals(updatedUsername)) {
            verify(userRepository, times(1)).existsByUsernameAndIdNot(updatedUsername, userId);
        }
        if (updatedEmail != null && !existingUser.getEmail().equals(updatedEmail)) {
            verify(userRepository, times(1)).existsByEmailAndIdNot(updatedEmail, userId);
        }
        // Verify encoding was called if password was provided and not null (service logic should handle this)
        if (updatedPassword != null) { // Assuming service encodes if password is not null
            verify(passwordEncoder, times(1)).encode(updatedPassword);
        }

        verify(userRepository, times(1)).save(any(User.class)); // Verify save was called
    }


    @Test
    void testUpdateUser_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        String userId = "nonexistentid";
        // Data doesn't matter for NotFound test, use nulls or empty collections
        String updatedUsername = "any";
        String updatedEmail = "any@example.com";
        String updatedPassword = "any";
        Set<Role> updatedRoles = Collections.emptySet();

        when(userRepository.findById(userId)).thenReturn(Optional.empty()); // Mock repo to return empty

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUser(userId, updatedUsername, updatedEmail, updatedPassword, updatedRoles);
        });

        // FIX: Updated assertion message to match actual service exception format
        assertThat(exception.getMessage()).isEqualTo("User not found with id : '" + userId + "'"); // Verify exception message

        // Verify only findById was called
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyString());
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_DuplicateUsername_ThrowsBadRequestException() {
        // Arrange
        String userId = "user1id";
        User existingUser = testUser; // Existing user in the database
        String updatedUsername = "existingadminusername"; // A username that already exists for another user

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsernameAndIdNot(updatedUsername, userId)).thenReturn(true); // Mock uniqueness check to fail

        String updatedEmail = existingUser.getEmail(); // Keep email same or different valid email
        String updatedPassword = null; // Don't update password
        Set<Role> updatedRoles = null; // Don't update roles


        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            userService.updateUser(userId, updatedUsername, updatedEmail, updatedPassword, updatedRoles);
        });

        // FIX: Updated assertion message to match actual service exception format
        assertThat(exception.getMessage()).isEqualTo("username is already in use"); // Verify exception message

        // Verify findById and username uniqueness check were called
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).existsByUsernameAndIdNot(updatedUsername, userId);
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyString()); // Email check should not be reached yet
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_DuplicateEmail_ThrowsBadRequestException() {
        // Arrange
        String userId = "user1id";
        User existingUser = testUser; // Existing user in the database
        String updatedEmail = "existingadminemail@example.com"; // An email that already exists for another user

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        // Mock uniqueness checks (assume username check passes if username is changed, or skipped if not)
        // Mock the email uniqueness check to fail
        when(userRepository.existsByEmailAndIdNot(updatedEmail, userId)).thenReturn(true);

        String updatedUsername = existingUser.getUsername(); // Keep username same or different valid username
        String updatedPassword = null; // Don't update password
        Set<Role> updatedRoles = null; // Don't update roles

        // Use lenient for username uniqueness check mock as it might not be called
        lenient().when(userRepository.existsByUsernameAndIdNot(anyString(), anyString())).thenReturn(false);


        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            userService.updateUser(userId, updatedUsername, updatedEmail, updatedPassword, updatedRoles);
        });

        // FIX: Updated assertion message to match actual service exception format
        assertThat(exception.getMessage()).isEqualTo("Email is already in use"); // Verify exception message

        // Verify findById, and email check were called
        verify(userRepository, times(1)).findById(userId);
        // Verify username check was called only if username changed, or skipped if null/same.
        if (updatedUsername != null && !existingUser.getUsername().equals(updatedUsername)) {
            verify(userRepository, times(1)).existsByUsernameAndIdNot(updatedUsername, userId);
        } else {
            verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyString());
        }
        verify(userRepository, times(1)).existsByEmailAndIdNot(updatedEmail, userId);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void testDeleteUser_Success() {
        // Arrange
        String userId = "user1id";
        // Mock finding the user first, as the service likely does this to check existence or perform other actions before deleting
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser)); // Mock repo to find the user

        // Act
        userService.deleteUser(userId);

        // Assert
        // Verify findById was called, then delete was called with the *fetched user object*
        verify(userRepository, times(1)).findById(userId);
        // FIX: Verify delete was called with the user object returned by findById mock
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    void testDeleteUser_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        String userId = "nonexistentid";
        // Mock finding the user to return empty, indicating it doesn't exist
        when(userRepository.findById(userId)).thenReturn(Optional.empty()); // Mock repo to return empty

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteUser(userId);
        });

        // FIX: Updated assertion message to match actual service exception format
        assertThat(exception.getMessage()).isEqualTo("User not found with id : '" + userId + "'"); // Verify exception message

        // Verify only findById was called, delete was not
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).delete(any(User.class)); // Verify delete was not called
    }
}