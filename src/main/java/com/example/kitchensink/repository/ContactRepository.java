package com.example.kitchensink.repository;

import com.example.kitchensink.model.Contact;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Contact entity operations
 */
@Repository
public interface ContactRepository extends MongoRepository<Contact, String> {
    List<Contact> findByCreatedBy(String username);
    
    boolean existsByEmailAndIdNot(String email, String id);
    
    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, String id);
}