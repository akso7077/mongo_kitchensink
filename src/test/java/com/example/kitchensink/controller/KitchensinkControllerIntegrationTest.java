package com.example.kitchensink.controller; // Place in src/test/java with your controllers

import com.example.kitchensink.exception.GlobalExceptionHandler; // Import for error response structure
import com.example.kitchensink.model.Contact; // Import Contact entity
import com.example.kitchensink.model.LoginRequest; // Import LoginRequest DTO
import com.example.kitchensink.model.SignUpRequest; // Import SignUpRequest DTO
import com.example.kitchensink.model.User; // Import User entity
import com.example.kitchensink.repository.ContactRepository; // Import ContactRepository for cleanup
import com.example.kitchensink.repository.UserRepository; // Import UserRepository for cleanup
import com.fasterxml.jackson.databind.ObjectMapper; // For converting objects to JSON
import org.junit.jupiter.api.BeforeEach; // For setup before each test
import org.junit.jupiter.api.Test; // For marking test methods
import org.springframework.beans.factory.annotation.Autowired; // For dependency injection
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; // To use MockMvc
import org.springframework.boot.test.context.SpringBootTest; // To load the full application context
import org.springframework.http.MediaType; // For setting content type
import org.springframework.test.web.servlet.MockMvc; // To simulate HTTP requests
import org.springframework.test.web.servlet.MvcResult; // To get response details
import org.springframework.test.web.servlet.ResultActions; // Import ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders; // To build requests (e.g., .get(), .post())
import org.springframework.test.web.servlet.result.MockMvcResultMatchers; // To assert on results
import org.springframework.test.context.ActiveProfiles; // Import for @ActiveProfiles

import java.util.List; // For response body of multiple contacts
import java.util.Map; // For validation error response body

import static org.assertj.core.api.Assertions.assertThat; // For assertions
// If you prefer JUnit assertions
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest // Loads the full Spring application context
@AutoConfigureMockMvc // Configures MockMvc
@ActiveProfiles("test") // FIX: Added @ActiveProfiles("test") as requested/observed
class KitchensinkControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // Injected to perform simulated HTTP requests

    @Autowired
    private UserRepository userRepository; // Injected for database cleanup
    @Autowired
    private ContactRepository contactRepository; // Injected for database cleanup

    @Autowired
    private ObjectMapper objectMapper; // Injected to convert Java objects to JSON and vice-versa

    // Define the structure of JwtAuthenticationResponse if it's an inner class in AuthController
    // If it's a separate class, remove this inner class and import the actual one.
    @lombok.Data // Assuming Lombok is used for this inner class
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class JwtAuthenticationResponse {
        private String accessToken;
        private String username;
        private java.util.Set<String> roles;
    }

    private String testUserToken; // To store the JWT token for the test user
    private String secondUserToken; // To store the JWT token for a second test user

    private final String TEST_USER_USERNAME = "testuserks";
    private final String TEST_USER_PASSWORD = "TestPass123!";
    private final String TEST_USER_EMAIL = "testuserks@example.com";

    private final String SECOND_USER_USERNAME = "seconduserks";
    private final String SECOND_USER_PASSWORD = "SecondPass456@";
    private final String SECOND_USER_EMAIL = "seconduserks@example.com";


    @BeforeEach // This method runs before each test method
    void setUp() throws Exception {
        // Clean both user and contact collections before each test
        userRepository.deleteAll();
        contactRepository.deleteAll();

        // Register and log in a test user to obtain a JWT token for authenticated tests
        registerUser(TEST_USER_USERNAME, TEST_USER_EMAIL, TEST_USER_PASSWORD);
        testUserToken = obtainJwtToken(TEST_USER_USERNAME, TEST_USER_PASSWORD);

        // Register and log in a second user for ownership tests
        registerUser(SECOND_USER_USERNAME, SECOND_USER_EMAIL, SECOND_USER_PASSWORD);
        secondUserToken = obtainJwtToken(SECOND_USER_USERNAME, SECOND_USER_PASSWORD);
    }

    // Helper method to register a user (copied from AuthControllerIntegrationTest)
    private void registerUser(String username, String email, String password) throws Exception {
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setUsername(username);
        signUpRequest.setEmail(email);
        signUpRequest.setPassword(password);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk()); // Assume registration is successful
    }

    // Helper method to log in a user and get the JWT token (copied from AuthControllerIntegrationTest)
    protected String obtainJwtToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JwtAuthenticationResponse authResponse = objectMapper.readValue(responseBody, JwtAuthenticationResponse.class);

        return authResponse.getAccessToken();
    }

    // Helper methods for authenticated requests (copied from AuthControllerIntegrationTest)
    protected ResultActions performAuthenticatedGet(String url, String jwtToken) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                .header("Authorization", "Bearer " + jwtToken)
                .accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performAuthenticatedPost(String url, Object requestBody, String jwtToken) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));
    }

    protected ResultActions performAuthenticatedPut(String url, Object requestBody, String jwtToken) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(url)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));
    }

    protected ResultActions performAuthenticatedDelete(String url, String jwtToken) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(url)
                .header("Authorization", "Bearer " + jwtToken));
    }


    // --- Test Cases for Kitchensink Contact Management (/api/kitchensink/contacts) ---

    @Test
    void testAccessSecuredEndpoint_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // Attempt to access /api/kitchensink/contacts (GET) without a token
        mockMvc.perform(MockMvcRequestBuilders.get("/api/kitchensink/contacts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized()); // Expect HTTP 401
    }


    @Test
    void testCreateContact_Success() throws Exception {
        Contact newContact = new Contact();
        newContact.setName("John Doe");
        newContact.setEmail("john.doe@example.com");
        newContact.setPhoneNumber("9876543210"); // Using a valid India phone number

        MvcResult result = performAuthenticatedPost("/api/kitchensink/contacts", newContact, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk()) // Expect HTTP 200 OK
                .andReturn();

        // Verify the response body is the created contact with an ID
        String responseBody = result.getResponse().getContentAsString();
        Contact createdContact = objectMapper.readValue(responseBody, Contact.class);

        assertThat(createdContact).isNotNull();
        assertThat(createdContact.getId()).isNotEmpty(); // Check if ID was assigned
        assertThat(createdContact.getName()).isEqualTo("John Doe");
        assertThat(createdContact.getCreatedBy()).isEqualTo(TEST_USER_USERNAME); // Verify createdBy is the authenticated user

        // Optional: Verify the contact was actually saved in the database
        assertThat(contactRepository.existsById(createdContact.getId())).isTrue();
    }

    @Test
    void testCreateContact_ValidationErrors() throws Exception {
        // Test case 1: Invalid contact data based on validation annotations
        Contact invalidContact = new Contact();
        invalidContact.setName("J"); // Name too short
        invalidContact.setEmail("invalid-email"); // Invalid email format
        invalidContact.setPhoneNumber("short"); // Phone number too short/invalid

        MvcResult result = performAuthenticatedPost("/api/kitchensink/contacts", invalidContact, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400
                .andReturn();

        // Verify the response body contains validation error details (Map<String, String>)
        String responseBody = result.getResponse().getContentAsString();
        Map<String, String> errors = objectMapper.readValue(responseBody, Map.class);

        assertThat(errors).containsKey("name");
        assertThat(errors).containsKey("email");
        assertThat(errors).containsKey("phoneNumber");

        // You can add more specific assertions on error messages if needed
    }

    @Test
    void testCreateContact_DuplicateEmailOrPhoneNumber() throws Exception {
        // Create a contact first
        Contact firstContact = new Contact();
        firstContact.setName("Existing Contact");
        firstContact.setEmail("existing.contact@example.com");
        firstContact.setPhoneNumber("9876543210"); // Using valid format
        performAuthenticatedPost("/api/kitchensink/contacts", firstContact, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Test case 1: Duplicate email by the same user
        Contact duplicateEmailContact = new Contact();
        duplicateEmailContact.setName("Another Contact");
        duplicateEmailContact.setEmail("existing.contact@example.com"); // Duplicate email
        duplicateEmailContact.setPhoneNumber("9999999999"); // Using a different valid phone

        MvcResult result = performAuthenticatedPost("/api/kitchensink/contacts", duplicateEmailContact, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400 from service
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        GlobalExceptionHandler.ErrorResponse errorResponse = objectMapper.readValue(responseBody, GlobalExceptionHandler.ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getMessage()).isEqualTo("Contact with this email already exists"); // Match your ContactService message


        // Test case 2: Duplicate phone number by the same user
        Contact duplicatePhoneContact = new Contact();
        duplicatePhoneContact.setName("Yet Another Contact");
        duplicatePhoneContact.setEmail("yetanother@example.com"); // Using a different valid email
        duplicatePhoneContact.setPhoneNumber("9876543210"); // Duplicate phone

        result = performAuthenticatedPost("/api/kitchensink/contacts", duplicatePhoneContact, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400 from service
                .andReturn();

        responseBody = result.getResponse().getContentAsString();
        errorResponse = objectMapper.readValue(responseBody, GlobalExceptionHandler.ErrorResponse.class);

        assertThat(errorResponse.getStatus()).isEqualTo(400);
        assertThat(errorResponse.getMessage()).isEqualTo("Contact with this phone number already exists"); // Match your ContactService message
    }


    @Test
    void testGetUserContacts_Success() throws Exception {
        // Create a few contacts for the test user
        Contact contact1 = new Contact();
        // FIX: Changed name to be valid according to regex
        contact1.setName("User Contact One");
        // FIX: Changed email to a simple valid format
        contact1.setEmail("user1a@example.com");
        contact1.setPhoneNumber("9876543210"); // Using valid India phone number

        // Add assertion to ensure creation succeeds and capture response to get ID
        // Changed to .andReturn() to capture response even on 400 for inspection
        MvcResult createResult1 = performAuthenticatedPost("/api/kitchensink/contacts", contact1, testUserToken)
                // .andExpect(MockMvcResultMatchers.status().isOk()) // Comment out this expectation initially to see the 400 body
                .andReturn();

        // Check the status explicitly
        int status1 = createResult1.getResponse().getStatus();

        // If status is not OK, print the response body for debugging
        if (status1 != 200) {
            String errorBody = createResult1.getResponse().getContentAsString();
            System.err.println("Contact 1 Creation Failed. Status: " + status1 + ", Body: " + errorBody);
            // Assert 200 so the test fails if it's still 400, but after logging the body
            assertThat(status1).isEqualTo(200);
        }

        // If status was 200, proceed to verify and create the next contact
        String responseBody1 = createResult1.getResponse().getContentAsString();
        Contact createdContact1 = objectMapper.readValue(responseBody1, Contact.class);
        String contact1Id = createdContact1.getId();
        // Verify saved to DB
        assertThat(contactRepository.existsById(contact1Id)).isTrue();


        Contact contact2 = new Contact();
        // FIX: Changed name to be valid according to regex
        contact2.setName("User Contact Two");
        // FIX: Changed email to a simple valid format
        contact2.setEmail("user2b@example.com");
        contact2.setPhoneNumber("9876543211"); // Using valid India phone number

        // Add assertion to ensure creation succeeds and capture response to get ID
        // Changed to .andReturn() to capture response even on 400 for inspection
        MvcResult createResult2 = performAuthenticatedPost("/api/kitchensink/contacts", contact2, testUserToken)
                // .andExpect(MockMvcResultMatchers.status().isOk()) // Comment out or remove
                .andReturn();

        // Check status for contact 2 creation
        int status2 = createResult2.getResponse().getStatus();
        if (status2 != 200) {
            String errorBody = createResult2.getResponse().getContentAsString();
            System.err.println("Contact 2 Creation Failed. Status: " + status2 + ", Body: " + errorBody);
            assertThat(status2).isEqualTo(200); // Re-assert 200
        }

        String responseBody2 = createResult2.getResponse().getContentAsString();
        Contact createdContact2 = objectMapper.readValue(responseBody2, Contact.class);
        String contact2Id = createdContact2.getId();
        // Verify saved to DB
        assertThat(contactRepository.existsById(contact2Id)).isTrue();


        // Create a contact for the second user (should not appear in the first user's list)
        Contact otherContact = new Contact();
        otherContact.setName("Other User Contact");
        otherContact.setEmail("other@example.com");
        otherContact.setPhoneNumber("9999999999"); // Using valid India phone number

        // Add assertion to ensure creation succeeds and capture response to get ID
        // Changed to .andReturn()
        MvcResult createResultOther = performAuthenticatedPost("/api/kitchensink/contacts", otherContact, secondUserToken)
                // .andExpect(MockMvcResultMatchers.status().isOk()) // Comment out or remove
                .andReturn();

        // Check status for other contact creation
        int statusOther = createResultOther.getResponse().getStatus();
        if (statusOther != 200) {
            String errorBody = createResultOther.getResponse().getContentAsString();
            System.err.println("Other Contact Creation Failed. Status: " + statusOther + ", Body: " + errorBody);
            assertThat(statusOther).isEqualTo(200); // Re-assert 200
        }

        String responseBodyOther = createResultOther.getResponse().getContentAsString();
        Contact createdContactOther = objectMapper.readValue(responseBodyOther, Contact.class);
        String otherContactId = createdContactOther.getId();
        // Verify saved to DB
        assertThat(contactRepository.existsById(otherContactId)).isTrue();


        // Get contacts for the first user
        MvcResult result = performAuthenticatedGet("/api/kitchensink/contacts", testUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk()) // Expect HTTP 200 OK
                .andReturn();

        // Verify the response body is a list containing only the first user's contacts
        String responseBody = result.getResponse().getContentAsString();
        List<Contact> userContacts = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Contact.class)); // Read as List<Contact>

        assertThat(userContacts).hasSize(2); // Should only contain the two contacts created by testUser
        assertThat(userContacts).extracting(Contact::getName).containsExactlyInAnyOrder("User Contact One", "User Contact Two");
        assertThat(userContacts).extracting(Contact::getCreatedBy).containsOnly(TEST_USER_USERNAME); // Ensure all belong to the user
    }

    @Test
    void testGetContactById_OwnedContact_Success() throws Exception {
        // Create a contact for the test user
        Contact contact = new Contact();
        contact.setName("Specific Owned Contact");
        contact.setEmail("owned@example.com");
        contact.setPhoneNumber("9876543212"); // FIX: Use valid India phone number
        MvcResult createResult = performAuthenticatedPost("/api/kitchensink/contacts", contact, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        String createdContactId = objectMapper.readValue(createResult.getResponse().getContentAsString(), Contact.class).getId();

        // Attempt to get the created contact by ID using the same user's token
        MvcResult getResult = performAuthenticatedGet("/api/kitchensink/contacts/" + createdContactId, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk()) // Expect HTTP 200 OK
                .andReturn();

        // Verify the response body is the correct contact
        String responseBody = getResult.getResponse().getContentAsString();
        Contact retrievedContact = objectMapper.readValue(responseBody, Contact.class);

        assertThat(retrievedContact).isNotNull();
        assertThat(retrievedContact.getId()).isEqualTo(createdContactId);
        assertThat(retrievedContact.getName()).isEqualTo("Specific Owned Contact");
        assertThat(retrievedContact.getCreatedBy()).isEqualTo(TEST_USER_USERNAME); // Verify ownership
    }

    @Test
    void testGetContactById_NotOwnedContact_ReturnsNotFound() throws Exception {
        // Create a contact for the SECOND user
        Contact otherUserContact = new Contact();
        otherUserContact.setName("Not My Contact");
        otherUserContact.setEmail("notmine@example.com");
        otherUserContact.setPhoneNumber("9999999990"); // FIX: Use valid India phone number
        MvcResult createResult = performAuthenticatedPost("/api/kitchensink/contacts", otherUserContact, secondUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        String otherContactId = objectMapper.readValue(createResult.getResponse().getContentAsString(), Contact.class).getId();

        // Attempt to get the other user's contact by ID using the first user's token
        performAuthenticatedGet("/api/kitchensink/contacts/" + otherContactId, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isNotFound()); // Expect HTTP 404
    }

    @Test
    void testGetContactById_NonExistent_ReturnsNotFound() throws Exception {
        // Attempt to get a contact with a non-existent ID
        performAuthenticatedGet("/api/kitchensink/contacts/nonexistentid123", testUserToken)
                .andExpect(MockMvcResultMatchers.status().isNotFound()); // Expect HTTP 404 (from service ResourceNotFoundException)
    }


    @Test
    void testUpdateContact_OwnedContact_Success() throws Exception {
        // Create a contact for the test user
        Contact contact = new Contact();
        contact.setName("Contact to Update");
        contact.setEmail("update@example.com");
        contact.setPhoneNumber("9876543213"); // FIX: Use valid India phone number
        MvcResult createResult = performAuthenticatedPost("/api/kitchensink/contacts", contact, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        Contact createdContact = objectMapper.readValue(createResult.getResponse().getContentAsString(), Contact.class);


        // Prepare updated data
        Contact updatedData = new Contact();
        updatedData.setName("Updated Contact Name");
        updatedData.setEmail("updated@example.com");
        updatedData.setPhoneNumber("9876543214"); // FIX: Use valid India phone number

        // Attempt to update the contact using the same user's token
        MvcResult updateResult = performAuthenticatedPut("/api/kitchensink/contacts/" + createdContact.getId(), updatedData, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk()) // Expect HTTP 200 OK
                .andReturn();

        // Verify the response body is the updated contact
        String responseBody = updateResult.getResponse().getContentAsString();
        Contact updatedContact = objectMapper.readValue(responseBody, Contact.class);

        assertThat(updatedContact).isNotNull();
        assertThat(updatedContact.getId()).isEqualTo(createdContact.getId()); // ID should remain the same
        assertThat(updatedContact.getName()).isEqualTo("Updated Contact Name"); // Verify name update
        assertThat(updatedContact.getEmail()).isEqualTo("updated@example.com"); // Verify email update
        assertThat(updatedContact.getPhoneNumber()).isEqualTo("9876543214"); // Verify phone update
        assertThat(updatedContact.getCreatedBy()).isEqualTo(TEST_USER_USERNAME); // Ownership should not change
    }

    @Test
    void testUpdateContact_OwnedContact_ValidationErrors() throws Exception {
        // Create a contact for the test user
        Contact contact = new Contact();
        contact.setName("Contact for Validation");
        contact.setEmail("validation@example.com");
        contact.setPhoneNumber("9876543215"); // FIX: Use valid India phone number
        MvcResult createResult = performAuthenticatedPost("/api/kitchensink/contacts", contact, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        Contact createdContact = objectMapper.readValue(createResult.getResponse().getContentAsString(), Contact.class);

        // Prepare invalid updated data
        Contact invalidUpdatedData = new Contact();
        invalidUpdatedData.setName("S"); // Name too short
        invalidUpdatedData.setEmail("bad-email"); // Invalid email format
        invalidUpdatedData.setPhoneNumber("short"); // Phone number too short/invalid

        // Attempt to update with invalid data
        MvcResult updateResult = performAuthenticatedPut("/api/kitchensink/contacts/" + createdContact.getId(), invalidUpdatedData, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Expect HTTP 400
                .andReturn();

        // Verify the response body contains validation error details
        String responseBody = updateResult.getResponse().getContentAsString();
        Map<String, String> errors = objectMapper.readValue(responseBody, Map.class);

        assertThat(errors).containsKey("name");
        assertThat(errors).containsKey("email");
        assertThat(errors).containsKey("phoneNumber");
    }


    @Test
    void testDeleteContact_OwnedContact_Success() throws Exception {
        // Create a contact for the test user
        Contact contactToDelete = new Contact();
        contactToDelete.setName("Contact to Delete");
        contactToDelete.setEmail("delete@example.com");
        contactToDelete.setPhoneNumber("9876543217"); // FIX: Use valid India phone number
        MvcResult createResult = performAuthenticatedPost("/api/kitchensink/contacts", contactToDelete, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        Contact createdContact = objectMapper.readValue(createResult.getResponse().getContentAsString(), Contact.class);

        // Verify the contact exists before deletion
        assertThat(contactRepository.existsById(createdContact.getId())).isTrue();

        // Attempt to delete the contact using the same user's token
        performAuthenticatedDelete("/api/kitchensink/contacts/" + createdContact.getId(), testUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk()) // Expect HTTP 200 OK
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Contact deleted successfully")); // Verify success message

        // Verify the contact is deleted from the database
        assertThat(contactRepository.existsById(createdContact.getId())).isFalse();
    }

    @Test
    void testDeleteContact_NotOwnedContact_ReturnsNotFound() throws Exception {
        // Create a contact for the SECOND user
        Contact otherUserContact = new Contact();
        otherUserContact.setName("Not My Contact Delete");
        otherUserContact.setEmail("notminedelete@example.com");
        otherUserContact.setPhoneNumber("9999999993"); // FIX: Use valid India phone number
        MvcResult createResult = performAuthenticatedPost("/api/kitchensink/contacts", otherUserContact, secondUserToken)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        String otherContactId = objectMapper.readValue(createResult.getResponse().getContentAsString(), Contact.class).getId();

        // Attempt to delete the other user's contact using the first user's token
        performAuthenticatedDelete("/api/kitchensink/contacts/" + otherContactId, testUserToken)
                .andExpect(MockMvcResultMatchers.status().isNotFound()); // Expect HTTP 404

        // Verify the contact was NOT deleted
        assertThat(contactRepository.existsById(otherContactId)).isTrue();
    }

    @Test
    void testDeleteContact_NonExistent_ReturnsNotFound() throws Exception {
        // Attempt to delete a contact with a non-existent ID
        performAuthenticatedDelete("/api/kitchensink/contacts/nonexistentid789", testUserToken)
                .andExpect(MockMvcResultMatchers.status().isNotFound()); // Expect HTTP 404 (from service ResourceNotFoundException)
    }


}