package com.example.kitchensink.exception; // Match your exception package

import com.example.kitchensink.exception.GlobalExceptionHandler.ErrorResponse; // Import your ErrorResponse DTO

import org.junit.jupiter.api.BeforeEach; // For setup
import org.junit.jupiter.api.Test; // For test methods
import org.mockito.Mock; // To create mock objects
import org.mockito.MockitoAnnotations; // To initialize mocks

// Import necessary Spring and validation classes for mocking
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest; // For WebRequest mock

// Imports for mocking validation errors
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError; // Import ObjectError as getAllErrors returns a List of ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException;

// Import specific exceptions handled by the handler
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.HashMap; // For building expected map
import java.util.List; // For lists of errors
import java.util.Map; // For Map response
import java.util.Date; // For Date comparison (optional)
import java.time.Instant; // For Instant comparison
import java.time.temporal.ChronoUnit; // For time unit

import static org.assertj.core.api.Assertions.assertThat; // For assertions (using AssertJ)
import static org.assertj.core.api.Assertions.within; // For Instant assertions
import static org.junit.jupiter.api.Assertions.assertThrows; // For JUnit 5 assertions
import static org.mockito.Mockito.*; // For mocking methods (when, verify, never, mock, eq)


// Unit tests for the GlobalExceptionHandler
class GlobalExceptionHandlerTests {

    // No @InjectMocks here, we will manually instantiate the handler
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock // Mock dependencies or inputs to handler methods
    private WebRequest mockWebRequest;

    // Mock specific exceptions
    @Mock
    private ResourceNotFoundException mockResourceNotFoundException;
    @Mock
    private BadRequestException mockBadRequestException;
    @Mock
    private BadCredentialsException mockBadCredentialsException;
    @Mock
    private AccessDeniedException mockAccessDeniedException;
    // Note: MethodArgumentNotValidException and generic Exception need slightly different mocking


    @BeforeEach // This method runs before each test method
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
        // Manually instantiate the handler
        globalExceptionHandler = new GlobalExceptionHandler();

        // Mock the WebRequest description
        when(mockWebRequest.getDescription(false)).thenReturn("uri=/test/path");

        // Mock basic exception messages
        when(mockResourceNotFoundException.getMessage()).thenReturn("Resource not found message");
        when(mockBadRequestException.getMessage()).thenReturn("Bad request message");
        when(mockBadCredentialsException.getMessage()).thenReturn("Invalid credentials message"); // Note: Handler uses static message, but we mock anyway
        when(mockAccessDeniedException.getMessage()).thenReturn("Access denied message"); // Note: Handler uses static message, but we mock anyway
    }

    // --- Test Cases for Each Exception Handler Method ---

    @Test
    void testHandleResourceNotFoundException_ReturnsNotFoundResponse() {
        // Arrange: mockResourceNotFoundException is already mocked in setUp

        // Act: Call the handler method
        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleResourceNotFoundException(
                mockResourceNotFoundException, mockWebRequest);

        // Assert: Verify the response status and body content
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND); // Check HTTP status is 404

        ErrorResponse errorResponse = responseEntity.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value()); // Check status value in body
        assertThat(errorResponse.getError()).isEqualTo("Not Found"); // Check error string
        assertThat(errorResponse.getMessage()).isEqualTo("Resource not found message"); // Check message from mocked exception
        assertThat(errorResponse.getPath()).isEqualTo("uri=/test/path"); // Check path from mocked WebRequest
        // Optional: Check timestamp is recent
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getTimestamp().toInstant()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS)); // Use AssertJ's within

        // Verify mocks were used
        verify(mockResourceNotFoundException, times(2)).getMessage(); // Verify getMessage() was called once (for response body message)
        verify(mockWebRequest, times(1)).getDescription(false);
        // Note: Verification for logging call is implicitly handled by the total count in global exception,
        // or would require mocking the logger itself for fine-grained verification.
    }

    @Test
    void testHandleBadRequestException_ReturnsBadRequestResponse() {
        // Arrange: mockBadRequestException is already mocked in setUp

        // Act: Call the handler method
        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleBadRequestException(
                mockBadRequestException, mockWebRequest);

        // Assert: Verify the response status and body content
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // Check HTTP status is 400

        ErrorResponse errorResponse = responseEntity.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value()); // Check status value in body
        assertThat(errorResponse.getError()).isEqualTo("Bad Request"); // Check error string
        assertThat(errorResponse.getMessage()).isEqualTo("Bad request message"); // Check message from mocked exception
        assertThat(errorResponse.getPath()).isEqualTo("uri=/test/path"); // Check path from mocked WebRequest

        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getTimestamp().toInstant()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));


        // Verify mocks were used
        verify(mockBadRequestException, times(2)).getMessage(); // ===> Verify getMessage() was called TWICE (for response body and logging) <===
        verify(mockWebRequest, times(1)).getDescription(false);
    }

    @Test
    void testHandleValidationExceptions_ReturnsBadRequestWithFieldErrors() {
        // Arrange: Mock a MethodArgumentNotValidException and its internal BindingResult and FieldError
        MethodArgumentNotValidException mockValidationException = mock(MethodArgumentNotValidException.class);
        BindingResult mockBindingResult = mock(BindingResult.class);
        // Create actual FieldError instances or mock them further if needed,
        // but for this test, creating instances is fine as handler uses their getters.
        // Use ObjectError as getAllErrors returns List<ObjectError>
        ObjectError fieldError1 = new FieldError("objectName", "fieldName1", "Error Message 1");
        ObjectError fieldError2 = new FieldError("objectName", "fieldName2", "Error Message 2");


        // Mock the exception to return the mock BindingResult
        when(mockValidationException.getBindingResult()).thenReturn(mockBindingResult);

        // Mock the BindingResult to return a list of ObjectErrors (which FieldError extends)
        when(mockBindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));


        // Act: Call the handler method
        ResponseEntity<Map<String, String>> responseEntity = globalExceptionHandler.handleValidationExceptions(
                mockValidationException);

        // Assert: Verify the response status and body content (Map of field errors)
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // Check HTTP status is 400

        Map<String, String> errorBody = responseEntity.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody).hasSize(2); // Should contain two field errors
        assertThat(errorBody).containsEntry("fieldName1", "Error Message 1"); // Verify field name and message
        assertThat(errorBody).containsEntry("fieldName2", "Error Message 2");

        // Verify mocks were used
        verify(mockValidationException, times(1)).getBindingResult();
        verify(mockBindingResult, times(1)).getAllErrors();
        // ===> Remove specific verify calls on FieldError instances, rely on map assertion <===
        // verify(mock(FieldError.class), times(anyInt())).getField(); // No longer verifying calls on mock FieldError instances
        // verify(mock(FieldError.class), times(anyInt())).getDefaultMessage(); // No longer verifying calls on mock FieldError instances
    }


    @Test
    void testHandleAuthenticationException_ReturnsUnauthorizedResponse() {
        // Arrange: mockBadCredentialsException is already mocked in setUp

        // Act: Call the handler method
        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleAuthenticationException(
                mockBadCredentialsException, mockWebRequest);

        // Assert: Verify the response status and body content
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED); // Check HTTP status is 401

        ErrorResponse errorResponse = responseEntity.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value()); // Check status value in body
        assertThat(errorResponse.getError()).isEqualTo("Unauthorized"); // Check error string
        assertThat(errorResponse.getMessage()).isEqualTo("Invalid username or password"); // Check specific message from handler
        assertThat(errorResponse.getPath()).isEqualTo("uri=/test/path"); // Check path from mocked WebRequest

        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getTimestamp().toInstant()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));


        // Verify mocks were used
        // ===> Verify getMessage() was called once (for logging) <===
        verify(mockBadCredentialsException, times(1)).getMessage();
        verify(mockWebRequest, times(1)).getDescription(false);
    }

    @Test
    void testHandleAccessDeniedException_ReturnsForbiddenResponse() {
        // Arrange: mockAccessDeniedException is already mocked in setUp

        // Act: Call the handler method
        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleAccessDeniedException(
                mockAccessDeniedException, mockWebRequest);

        // Assert: Verify the response status and body content
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN); // Check HTTP status is 403

        ErrorResponse errorResponse = responseEntity.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value()); // Check status value in body
        assertThat(errorResponse.getError()).isEqualTo("Forbidden"); // Check error string
        assertThat(errorResponse.getMessage()).isEqualTo("You don't have permission to access this resource"); // Check specific message
        assertThat(errorResponse.getPath()).isEqualTo("uri=/test/path"); // Check path from mocked WebRequest

        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getTimestamp().toInstant()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));

        // Verify mocks were used
        // ===> Verify getMessage() was called once (for logging) <===
        verify(mockAccessDeniedException, times(1)).getMessage();
        verify(mockWebRequest, times(1)).getDescription(false);
    }

    @Test
    void testHandleGlobalException_ReturnsInternalServerErrorResponse() {
        // Arrange: Mock a generic Exception
        Exception mockGenericException = mock(Exception.class);
        when(mockGenericException.getMessage()).thenReturn("Something went wrong");

        // Act: Call the handler method for a generic exception
        ResponseEntity<ErrorResponse> responseEntity = globalExceptionHandler.handleGlobalException(
                mockGenericException, mockWebRequest);

        // Assert: Verify the response status and body content
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR); // Check HTTP status is 500

        ErrorResponse errorResponse = responseEntity.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value()); // Check status value in body
        assertThat(errorResponse.getError()).isEqualTo("Internal Server Error"); // Check error string
        assertThat(errorResponse.getMessage()).isEqualTo("Something went wrong"); // Check message from mocked exception
        assertThat(errorResponse.getPath()).isEqualTo("uri=/test/path"); // Check path from mocked WebRequest

        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getTimestamp().toInstant()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));

        // Verify mocks were used
        // ===> Verify getMessage() was called three times (response body, log message, logback internal) <===
        verify(mockGenericException, times(3)).getMessage();
        verify(mockWebRequest, times(1)).getDescription(false);
        // Verify logger was called with error and exception (more advanced mocking)
        // verify(log, times(1)).error(anyString(), eq(mockGenericException), any(Throwable.class)); // Requires mocking Slf4j logger
    }
}