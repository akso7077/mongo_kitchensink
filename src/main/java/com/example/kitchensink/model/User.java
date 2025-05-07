package com.example.kitchensink.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority; // Import GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Import SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails; // Import UserDetails

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Collection; // Import Collection
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors; // Import Collectors

/**
 * User entity that represents application users with authentication details,
 * implements UserDetails for Spring Security
 */
@Data // Generates getters, setters, equals, hashCode, toString
@NoArgsConstructor // Generates no-arg constructor
@AllArgsConstructor // Generates constructor with all fields
@Document(collection = "users")
public class User implements UserDetails { // Implement UserDetails
    @Id
    private String id;

    @NotBlank
    @Size(min = 8, max = 30, message = "Username must be between 8 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username can only contain letters and numbers")
    private String username;

    @NotBlank
    @Size(min = 8, max = 120)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character (!@#$%^&*()_+-=)")
    private String password;

    @NotBlank
    @Size(max = 100)
    @Email(message = "Please enter a valid email address")
    private String email;

    private Set<Role> roles = new HashSet<>(); // Initialize to prevent null issues

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // Your existing specific constructor - Keep this exactly as you had it
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        // Roles are set externally in UserService after object creation
    }


    // --- UserDetails Interface Methods ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // This method maps your Set<Role> to Spring Security's Collection<? extends GrantedAuthority>
        // It correctly uses the 'roles' field from your existing class
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name())) // Assuming Role enum has a name() or toString() giving "ROLE_USER" etc.
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return this.password; // Return your existing password field
    }

    @Override
    public String getUsername() {
        return this.username; // Return your existing username field
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Assuming user accounts do not expire in this app
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Assuming user accounts are not locked in this app
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Assuming user credentials (password) don't expire independently of tokens
    }

    @Override
    public boolean isEnabled() {
        return true; // Assuming users are enabled by default
    }

    // Your existing getters, setters, equals, hashCode, toString are handled by @Data
}
