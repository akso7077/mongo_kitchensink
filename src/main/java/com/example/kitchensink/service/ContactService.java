package com.example.kitchensink.service;

import com.example.kitchensink.exception.BadRequestException;
import com.example.kitchensink.exception.ResourceNotFoundException;
import com.example.kitchensink.model.Contact;
import com.example.kitchensink.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service for managing contacts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository contactRepository;

    /**
     * Create a new contact
     * 
     * @param contact Contact to create
     * @param username Username of creator
     * @return Created contact
     */
    public Contact createContact(Contact contact, String username) {
        // Check if email already exists
        if (contactRepository.existsByEmail(contact.getEmail())) {
            throw new BadRequestException("Contact with this email already exists");
        }

        if (contactRepository.existsByPhoneNumber(contact.getPhoneNumber())){
            throw new BadRequestException("Contact with this phone number already exists");
        }

        contact.setCreatedBy(username);
        contact.setCreatedAt(Instant.now());
        contact.setUpdatedAt(Instant.now());
        
        Contact savedContact = contactRepository.save(contact);
        log.info("Created new contact: {} by user: {}", contact.getName(), username);
        return savedContact;
    }

    /**
     * Get all contacts
     * 
     * @return List of all contacts
     */
    public List<Contact> getAllContacts() {
        return contactRepository.findAll();
    }

    /**
     * Get contacts created by a specific user
     * 
     * @param username Username of creator
     * @return List of contacts
     */
    public List<Contact> getContactsByUser(String username) {
        return contactRepository.findByCreatedBy(username);
    }

    /**
     * Get contact by ID
     * 
     * @param id Contact ID
     * @return Contact object
     */
    public Contact getContactById(String id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", id));
    }

    /**
     * Update an existing contact
     * 
     * @param id Contact ID
     * @param updatedContact Updated contact details
     * @return Updated contact
     */
    public Contact updateContact(String id, Contact updatedContact) {
        Contact contact = getContactById(id);
        
        // Check if the updated email already exists for another contact
        if (!contact.getEmail().equals(updatedContact.getEmail()) && 
                contactRepository.existsByEmailAndIdNot(updatedContact.getEmail(), id)) {
            throw new BadRequestException("Email is already in use");
        }

        if (!contact.getPhoneNumber().equals(updatedContact.getPhoneNumber()) &&
                contactRepository.existsByPhoneNumberAndIdNot(updatedContact.getPhoneNumber(), id)){
            throw new BadRequestException("Phone number is already in use");
        }

        contact.setName(updatedContact.getName());
        contact.setEmail(updatedContact.getEmail());
        contact.setPhoneNumber(updatedContact.getPhoneNumber());
        contact.setUpdatedAt(Instant.now());
        
        Contact saved = contactRepository.save(contact);
        log.info("Updated contact with id: {}", id);
        return saved;
    }

    /**
     * Delete a contact
     * 
     * @param id Contact ID
     */
    public void deleteContact(String id) {
        Contact contact = getContactById(id);
        contactRepository.delete(contact);
        log.info("Deleted contact with id: {}", id);
    }
}
