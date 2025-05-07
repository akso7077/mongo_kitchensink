package com.example.kitchensink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Main entry point for the Kitchen Sink application.
 * This application demonstrates a Spring Boot application with:
 * - JWT-based authentication
 * - MongoDB integration
 * - Role-based access control
 * - Exception handling
 * - API documentation with Swagger
 * - User management functionality
 */
@SpringBootApplication
@EnableMongoAuditing
public class KitchensinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(KitchensinkApplication.class, args);
    }
}