package com.example.kitchensink.repository;

import com.example.kitchensink.model.RefreshToken;
import com.example.kitchensink.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing RefreshToken documents
 */
@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token); // Find a token by its string value

    void deleteByUser(User user); // Delete all tokens for a specific user (e.g., on logout)

    // Add other necessary methods later if needed, e.g., for cleaning up expired tokens
}