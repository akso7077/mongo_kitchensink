package com.example.kitchensink.service; // This should match the package of your CustomUserDetailsService

import com.example.kitchensink.model.User; // Import your User model
// ===> IMPORT YOUR Role ENUM <===
import com.example.kitchensink.model.Role; // Import your Role enum


import com.example.kitchensink.repository.UserRepository; // Import your UserRepository

import org.junit.jupiter.api.BeforeEach; // For setup
import org.junit.jupiter.api.Test; // For test methods
import org.mockito.InjectMocks; // To inject mocks
import org.mockito.Mock; // To create mocks
import org.mockito.MockitoAnnotations; // To initialize mocks
import org.springframework.security.core.GrantedAuthority; // Import
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Import
import org.springframework.security.core.userdetails.UserDetails; // Import
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Import

import java.util.HashSet; // Import
import java.util.Optional; // Import
import java.util.Set; // Import

import static org.assertj.core.api.Assertions.assertThat; // For assertions (using AssertJ)
import static org.junit.jupiter.api.Assertions.assertThrows; // For asserting exceptions (using JUnit 5)
import static org.mockito.ArgumentMatchers.anyString; // For mocking string arguments
import static org.mockito.Mockito.when; // For mocking method calls
import static org.mockito.Mockito.verify; // For verifying method calls
import static org.mockito.Mockito.times; // For verifying call count
import static org.mockito.Mockito.eq; // For verifying exact arguments


// This is the unit test class for your CustomUserDetailsService
class CustomUserDetailsServiceTests { // Renamed from UserDetailsServiceImplTests

    @InjectMocks // Inject mocks into this instance - This is the class under test
    private CustomUserDetailsService customUserDetailsService; // ===> Match YOUR class name and variable name <===

    @Mock // Mock dependencies of CustomUserDetailsService
    private UserRepository userRepository; // Mock your UserRepository


    @BeforeEach // This method runs before each test method
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    // --- Test Cases for loadUserByUsername ---

    @Test
    void testLoadUserByUsername_UserFound_ReturnsUserDetails() {
        // Arrange: Prepare a mock User entity that will be returned by the repository
        String username = "testuser";
        String encodedPassword = "encodedPassword";
        // ===> Use no-arg constructor and setters as needed for your User model <===
        User mockUser = new User(); // Assuming @NoArgsConstructor
        mockUser.setUsername(username);
        mockUser.setPassword(encodedPassword);
        mockUser.setId("userId123"); // Optional, but good practice to set ID in mocks

        Set<Role> roles = new HashSet<>();
        // ===> Use the Role enum constants directly <===
        roles.add(Role.ROLE_USER); // Use the enum constant directly
        roles.add(Role.ROLE_ADMIN); // Use the enum constant directly
        mockUser.setRoles(roles); // Assuming setRoles method exists


        // Mock repository behavior: Tell Mockito what userRepository.findByUsername should return
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(mockUser));


        // Act: Call the method under test
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        // Assert: Verify that the returned UserDetails is correct
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(username);
        assertThat(userDetails.getPassword()).isEqualTo(encodedPassword);
        // Verify authorities (roles). Your CustomUserDetailsService maps Role enum name (role.name()) to SimpleGrantedAuthority strings.
        assertThat(userDetails.getAuthorities()).hasSize(2);
        // Check that the authority strings match the expected enum names (standard Enum.name() output)
        assertThat(userDetails.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");


        // Verify repository method was called: Ensure loadUserByUsername called findByUsername
        verify(userRepository, times(1)).findByUsername(eq(username)); // Verify findByUsername was called once with the correct username
    }

    @Test
    void testLoadUserByUsername_UserNotFound_ThrowsUsernameNotFoundException() {
        // Arrange: Prepare for a scenario where the user is not found.
        String nonExistentUsername = "nonexistentuser";

        // Mock repository behavior: Tell Mockito that userRepository.findByUsername should return an empty Optional
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());


        // Act & Assert: Call the method under test and assert that it throws UsernameNotFoundException
        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername(nonExistentUsername);
        });

        // Verify repository method was called: Ensure loadUserByUsername called findByUsername
        verify(userRepository, times(1)).findByUsername(eq(nonExistentUsername)); // Verify findByUsername was called once with the correct username
    }
}