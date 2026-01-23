
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Copiamos el archivo pom.xml para descargar las dependencias primero (optimiza el cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el resto del código fuente y construimos el JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copiamos el JAR generado desde la etapa de construcción
# El nombre debe coincidir con el de tu pom.xml: nexus-0.0.1-SNAPSHOT.jar
COPY --from=build /app/target/nexus-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Comando para arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]