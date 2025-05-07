package com.example.kitchensink.controller;

import com.example.kitchensink.model.Contact;
import com.example.kitchensink.model.Role;
import com.example.kitchensink.model.User;
import com.example.kitchensink.service.ContactService;
import com.example.kitchensink.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.kitchensink.exception.BadRequestException;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * Controller for admin operations
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
@Tag(name = "Admin", description = "Admin operations API")
@SecurityRequirement(name = "bearer-jwt")
public class AdminController {

    private final ContactService contactService;
    private final UserService userService;

    /**
     * Get all contacts
     *
     * @return List of all contacts
     */
    @GetMapping("/contacts")
    @Operation(summary = "Get all contacts", description = "Get all contacts in the system")
    public ResponseEntity<List<Contact>> getAllContacts() {
        List<Contact> contacts = contactService.getAllContacts();
        return ResponseEntity.ok(contacts);
    }

    /**
     * Get contact by ID
     *
     * @param id Contact ID
     * @return Contact data
     */
    @GetMapping("/contacts/{id}")
    @Operation(summary = "Get contact", description = "Get contact by ID")
    public ResponseEntity<Contact> getContact(@PathVariable String id) {
        Contact contact = contactService.getContactById(id);
        return ResponseEntity.ok(contact);
    }

    /**
     * Update a contact
     *
     * @param id Contact ID
     * @param contact Updated contact data
     * @return Updated contact
     */
    @PutMapping("/contacts/{id}")
    @Operation(summary = "Update contact", description = "Update an existing contact")
    public ResponseEntity<Contact> updateContact(
            @PathVariable String id,
            @Valid @RequestBody Contact contact) {
        Contact updatedContact = contactService.updateContact(id, contact);
        return ResponseEntity.ok(updatedContact);
    }

    /**
     * Delete a contact
     *
     * @param id Contact ID
     * @return Success message
     */
    @DeleteMapping("/contacts/{id}")
    @Operation(summary = "Delete contact", description = "Delete a contact")
    public ResponseEntity<?> deleteContact(@PathVariable String id) {
        contactService.deleteContact(id);
        return ResponseEntity.ok(new MessageResponse("Contact deleted successfully"));
    }

    /**
     * Get all users
     *
     * @return List of all users
     */
    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Get all users in the system")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID
     *
     * @param id User ID
     * @return User data
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Get user", description = "Get user by ID")
    public ResponseEntity<User> getUser(@PathVariable String id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Data transfer object for user updates
     */
    @lombok.Data
    public static class UserUpdateRequest {
        @NotBlank // Assuming this is also needed for updates
        @Size(min = 8, max = 30, message = "Username must be between 8 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username can only contain letters and numbers")
        private String username;
        @Email(message = "Please enter a valid email address")
        private String email;
        private String password;
        private Set<Role> roles;
    }

    /**
     * Update a user
     *
     * @param id User ID
     * @param updateRequest Updated user data
     * @return Updated user
     */
    @PutMapping("/users/{id}")
    @Operation(summary = "Update user", description = "Update an existing user")
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails currentUserDetails = (UserDetails) authentication.getPrincipal();
            String currentUsername = currentUserDetails.getUsername();
            User currentUserEntity = userService.getUserByUsername(currentUsername);
            if (currentUserEntity != null && currentUserEntity.getId().equals(id)) {
                log.warn("Admin user {} attempted to edit their own account (ID: {})", currentUsername, id);
                throw new BadRequestException("You cannot edit your own user account.");
            }
        }
        else{
            log.error("Could not retrieve authenticated user details from SecurityContext.");
        }
        User updatedUser = userService.updateUser(
                id,
                updateRequest.getUsername(),
                updateRequest.getEmail(),
                updateRequest.getPassword(),
                updateRequest.getRoles());
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete a user
     *
     * @param id User ID
     * @return Success message
     */
    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user", description = "Delete a user")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }

    /**
     * Simple response message class
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class MessageResponse {
        private String message;
    }
}