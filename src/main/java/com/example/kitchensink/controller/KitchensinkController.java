package com.example.kitchensink.controller;

import com.example.kitchensink.model.Contact;
import com.example.kitchensink.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller for the main "kitchensink" functionality - managing contacts
 */
@RestController
@RequestMapping("/api/kitchensink")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Kitchen Sink", description = "Kitchen Sink Contact Management API")
@SecurityRequirement(name = "bearer-jwt")
public class KitchensinkController {

    private final ContactService contactService;

    /**
     * Create a new contact
     *
     * @param contact Contact data
     * @param authentication Current authentication
     * @return Created contact
     */
    @PostMapping("/contacts")
    @Operation(summary = "Create contact", description = "Create a new contact")
    public ResponseEntity<Contact> createContact(
            @Valid @RequestBody Contact contact,
            Authentication authentication) {
        String username = authentication.getName();
        Contact createdContact = contactService.createContact(contact, username);
        return ResponseEntity.ok(createdContact);
    }

    /**
     * Get contacts for the current user
     *
     * @param authentication Current authentication
     * @return List of contacts
     */
    @GetMapping("/contacts")
    @Operation(summary = "Get user contacts", description = "Get all contacts created by the current user")
    public ResponseEntity<List<Contact>> getUserContacts(Authentication authentication) {
        String username = authentication.getName();
        List<Contact> contacts = contactService.getContactsByUser(username);
        return ResponseEntity.ok(contacts);
    }

    /**
     * Get contact by ID (only if created by current user)
     *
     * @param id Contact ID
     * @param authentication Current authentication
     * @return Contact data
     */
    @GetMapping("/contacts/{id}")
    @Operation(summary = "Get contact", description = "Get contact by ID")
    public ResponseEntity<Contact> getContact(
            @PathVariable String id,
            Authentication authentication) {
        Contact contact = contactService.getContactById(id);

        // Check if the contact belongs to the current user
        if (!contact.getCreatedBy().equals(authentication.getName())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(contact);
    }

    /**
     * Update a contact (only if created by current user)
     *
     * @param id Contact ID
     * @param contact Updated contact data
     * @param authentication Current authentication
     * @return Updated contact
     */
    @PutMapping("/contacts/{id}")
    @Operation(summary = "Update contact", description = "Update a contact created by the current user")
    public ResponseEntity<Contact> updateContact(
            @PathVariable String id,
            @Valid @RequestBody Contact contact,
            Authentication authentication) {

        Contact existingContact = contactService.getContactById(id);

        // Check if the contact belongs to the current user
        if (!existingContact.getCreatedBy().equals(authentication.getName())) {
            return ResponseEntity.notFound().build();
        }

        Contact updatedContact = contactService.updateContact(id, contact);
        return ResponseEntity.ok(updatedContact);
    }

    /**
     * Delete a contact (only if created by current user)
     *
     * @param id Contact ID
     * @param authentication Current authentication
     * @return Success message
     */
    @DeleteMapping("/contacts/{id}")
    @Operation(summary = "Delete contact", description = "Delete a contact created by the current user")
    public ResponseEntity<?> deleteContact(
            @PathVariable String id,
            Authentication authentication) {

        Contact contact = contactService.getContactById(id);

        // Check if the contact belongs to the current user
        if (!contact.getCreatedBy().equals(authentication.getName())) {
            return ResponseEntity.notFound().build();
        }

        contactService.deleteContact(id);
        return ResponseEntity.ok(new MessageResponse("Contact deleted successfully"));
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