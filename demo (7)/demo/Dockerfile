# =====================================================================
# Multi-stage Dockerfile cho Spring Boot 3 (Java 21) - Production Ready
# =====================================================================

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Chạy dưới non-root user tăng cường bảo mật container
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
