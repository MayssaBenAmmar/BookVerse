# Option 1: Multi-stage build (builds from source - recommended for deployment)
FROM maven:3.9.4-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory for file storage
RUN mkdir -p uploads

# Fix: Use port 8088 (from your application.yml)
EXPOSE 8088

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# -------------------------------------------------------------------
# Option 2: Simple build (if you already have the JAR - for quick testing)
# FROM eclipse-temurin:17-jre-alpine
# WORKDIR /app
# COPY target/book-network-0.0.1-SNAPSHOT.jar app.jar
# RUN mkdir -p uploads
# EXPOSE 8088
# ENTRYPOINT ["java", "-jar", "app.jar"]