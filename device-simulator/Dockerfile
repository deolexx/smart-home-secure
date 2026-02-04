# Multi-stage build for Device Simulator
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy all pom files first (for dependency caching)
COPY pom.xml .
COPY hub/pom.xml ./hub/
COPY device-simulator/pom.xml ./device-simulator/

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -f pom.xml -pl device-simulator -am || true

# Copy source code
COPY device-simulator/src ./device-simulator/src

# Build the application
RUN mvn clean package -f pom.xml -pl device-simulator -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/device-simulator/target/*-jar-with-dependencies.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
