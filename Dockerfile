# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw -B -q package -DskipTests 2>/dev/null || \
    (apk add --no-cache maven && mvn -B -q package -DskipTests)

# ── Stage 2: runtime (distroless — sem shell, menor superfície de ataque) ───
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuário não-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
