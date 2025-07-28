# Use an OpenJDK base image
FROM eclipse-temurin:17-jdk as build

# Create app directory
WORKDIR /app

# Copy source files
COPY . .

# Build your Spring Boot app using Maven
RUN ./mvnw clean package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy only the built .jar from previous stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8082

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
