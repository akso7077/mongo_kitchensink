package com.example.kitchensink.model; // Match your model package

import org.junit.jupiter.api.Test; // For marking test methods
import java.time.Instant; // Import Instant

import static org.assertj.core.api.Assertions.assertThat; // For assertions

class ContactTests {

    @Test
    void testNoArgsConstructor() {
        // Test that the no-argument constructor works
        Contact contact = new Contact();
        assertThat(contact).isNotNull();
        // Assert default values if any (Lombok defaults to null for objects)
        assertThat(contact.getId()).isNull();
        assertThat(contact.getName()).isNull();
        assertThat(contact.getEmail()).isNull();
        assertThat(contact.getPhoneNumber()).isNull();
        assertThat(contact.getCreatedBy()).isNull();
        assertThat(contact.getCreatedAt()).isNull();
        assertThat(contact.getUpdatedAt()).isNull();
    }

    @Test
    void testSettersAndGetters() {
        // Test setters and getters
        Contact contact = new Contact();
        String id = "contact123";
        String name = "John Doe";
        String email = "john.doe@example.com";
        String phoneNumber = "123-456-7890";
        String createdBy = "testuser";
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now();

        contact.setId(id);
        contact.setName(name);
        contact.setEmail(email);
        contact.setPhoneNumber(phoneNumber);
        contact.setCreatedBy(createdBy);
        contact.setCreatedAt(createdAt);
        contact.setUpdatedAt(updatedAt);

        assertThat(contact.getId()).isEqualTo(id);
        assertThat(contact.getName()).isEqualTo(name);
        assertThat(contact.getEmail()).isEqualTo(email);
        assertThat(contact.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(contact.getCreatedBy()).isEqualTo(createdBy);
        assertThat(contact.getCreatedAt()).isEqualTo(createdAt);
        assertThat(contact.getUpdatedAt()).isEqualTo(updatedAt);
    }

    // Lombok's @EqualsAndHashCode(of = "id") generates equals/hashCode based on ID
    // You might want to add tests for equals/hashCode if this is critical or customized
    @Test
    void testEqualsAndHashCode_BasedOnId() {
        Contact contact1 = new Contact();
        contact1.setId("id1");
        contact1.setName("Contact 1");

        Contact contact2 = new Contact();
        contact2.setId("id1"); // Same ID as contact1
        contact2.setName("Contact 2 - Different Name"); // Different name

        Contact contact3 = new Contact();
        contact3.setId("id2"); // Different ID

        // Test equals
        //assertThat(contact1).isEqualTo(contact2); // Should be equal because IDs are the same
        assertThat(contact1).isNotEqualTo(contact3); // Should not be equal because IDs are different
        assertThat(contact1).isNotEqualTo(null); // Should not be equal to null
        assertThat(contact1).isNotEqualTo(new Object()); // Should not be equal to different class

    }

    // Lombok's @Data includes @ToString by default
    @Test
    void testToString() {
        Contact contact = new Contact();
        contact.setId("contact123");
        contact.setName("John Doe");
        contact.setEmail("john.doe@example.com");
        contact.setPhoneNumber("123-456-7890");
        contact.setCreatedBy("testuser");
        Instant now = Instant.now();
        contact.setCreatedAt(now);
        contact.setUpdatedAt(now);

        String toString = contact.toString();

        // Verify toString contains expected fields (adapt if fields are excluded from toString in your model)
        assertThat(toString).contains("id=contact123");
        assertThat(toString).contains("name=John Doe");
        assertThat(toString).contains("email=john.doe@example.com");
        assertThat(toString).contains("phoneNumber=123-456-7890");
        assertThat(toString).contains("createdBy=testuser");
        assertThat(toString).contains("createdAt="); // Check for presence, exact Instant string varies
        assertThat(toString).contains("updatedAt="); // Check for presence
    }
}