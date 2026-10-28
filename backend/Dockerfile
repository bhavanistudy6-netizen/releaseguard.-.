# Multi-stage build for ReleaseGuard Backend

# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy POM and download dependencies for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy sources and package Spring Boot executable JAR
COPY src src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S releaseguard && adduser -S releaseguard -G releaseguard
USER releaseguard:releaseguard
WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder --chown=releaseguard:releaseguard /workspace/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-Duser.timezone=UTC -XX:+UseG1GC"

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
