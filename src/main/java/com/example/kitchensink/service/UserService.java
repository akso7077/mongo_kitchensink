package com.example.kitchensink.service;

import com.example.kitchensink.exception.BadRequestException;
import com.example.kitchensink.exception.ResourceNotFoundException;
import com.example.kitchensink.model.Role;
import com.example.kitchensink.model.User;
import com.example.kitchensink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for managing users
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user with USER role
     *
     * @param username Username
     * @param email Email
     * @param password Raw password
     * @return Created user
     */
    public User createUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already in use");
        }

        User user = new User(username, email, passwordEncoder.encode(password));
        user.setRoles(Collections.singleton(Role.ROLE_USER));

        User savedUser = userRepository.save(user);
        log.info("Created new user: {}", username);
        return savedUser;
    }

    /**
     * Create an admin user
     *
     * @param username Username
     * @param email Email
     * @param password Raw password
     * @return Created user
     */
    public User createAdminUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already in use");
        }

        User user = new User(username, email, passwordEncoder.encode(password));
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_USER);
        roles.add(Role.ROLE_ADMIN);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("Created new admin user: {}", username);
        return savedUser;
    }

    /**
     * Get all users
     *
     * @return List of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get user by username
     *
     * @param username Username
     * @return User object
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Get user by ID
     *
     * @param id User ID
     * @return User object
     */
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    /**
     * Update an existing user
     *
     * @param id User ID
     * @param email Updated email
     * @param password Updated password (can be null if not changing)
     * @param roles Updated roles (can be null if not changing)
     * @return Updated user
     */
    public User updateUser(String id, String username, String email, String password, Set<Role> roles) {
        User user = getUserById(id);

        // Check if the updated email already exists for another user
        if (email != null && !user.getEmail().equals(email) &&
                userRepository.existsByEmailAndIdNot(email, id)) {
            throw new BadRequestException("Email is already in use");
        }

        if (username != null && !user.getUsername().equals(username) &&
                userRepository.existsByUsernameAndIdNot(username, id)) {
            throw new BadRequestException("username is already in use");
        }
        else{
            user.setUsername(username);
        }

        if (email != null) {
            user.setEmail(email);
        }

        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        if (roles != null && !roles.isEmpty()) {
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);
        log.info("Updated user with id: {}", id);
        return savedUser;
    }

    /**
     * Delete a user
     *
     * @param id User ID
     */
    public void deleteUser(String id) {
        User user = getUserById(id);
        userRepository.delete(user);
        log.info("Deleted user with id: {}", id);
    }
}