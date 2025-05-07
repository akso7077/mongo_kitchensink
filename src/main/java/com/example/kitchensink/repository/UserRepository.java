package com.example.kitchensink.repository;

import com.example.kitchensink.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository; // Import the Repository annotation

import java.util.Optional;

/**
 * Repository for User entity operations
 */
@Repository // Add this annotation
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    Boolean existsByUsernameAndIdNot(String username, String id);

    Boolean existsByEmailAndIdNot(String email, String id);
}