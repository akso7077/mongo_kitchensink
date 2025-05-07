# Stage 1: Build the application
# Use a base image that contains a JDK and Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set the working directory inside the builder stage
WORKDIR /app

# Copy the Maven project files (pom.xml) first to leverage Docker layer caching
# If pom.xml doesn't change, this layer (and subsequent dependency download) is cached
COPY pom.xml .

# Download dependencies (only if pom.xml changed)
RUN mvn dependency:go-offline

# Copy the rest of the application source code
COPY src ./src

# Package the application into an executable JAR
# The -DskipTests flag is common for Docker builds, remove if you want tests to run
RUN mvn clean package -DskipTests

# ---

# Stage 2: Run the application
# Use a lightweight JRE image as the base for the final image
FROM eclipse-temurin:21-jre-jammy

# Set the working directory for the runner stage
WORKDIR /app

# Copy the executable JAR file from the builder stage's target directory
# The JAR name follows the pattern artifactId-version.jar from your pom.xml
COPY --from=builder /app/target/kitchensink-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that your Spring Boot application listens on (default is 8080)
EXPOSE 8084

# Define the command to run your application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
