# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -q -Dmaven.compiler.fork=false

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/nexus-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-Xmx380m", "-Xms48m", "-Xss256k", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxMetaspaceSize=120m", \
  "-XX:CompressedClassSpaceSize=64m", \
  "-XX:+UseSerialGC", \
  "-XX:+DisableExplicitGC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.backgroundpreinitializer.ignore=true", \
  "-Dfile.encoding=UTF-8", \
  "-jar", "app.jar"]
