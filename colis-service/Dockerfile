# ─── Stage 1 : Build ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copier le pom.xml parent et le modifier pour supprimer les autres modules
# On ne garde que colis-service dans la déclaration <modules>
COPY pom.xml ./pom.xml
RUN sed -i 's|<module>api-gateway</module>||g' pom.xml && \
    sed -i 's|<module>user-service</module>||g' pom.xml && \
    sed -i 's|<module>livreur-service</module>||g' pom.xml && \
    sed -i 's|<module>notification-service</module>||g' pom.xml

# Copier le pom.xml du module colis-service
COPY colis-service/pom.xml ./colis-service/pom.xml

# Télécharger les dépendances (le parent ne déclare plus que colis-service)
RUN mvn dependency:go-offline -pl colis-service -q

# Copier le code source et compiler
COPY colis-service/src ./colis-service/src
RUN mvn clean package -pl colis-service -DskipTests -q

# ─── Stage 2 : Runtime ───────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/colis-service/target/*.jar app.jar

EXPOSE 8082

ENV DB_USERNAME=root \
    DB_PASSWORD=root \
    SPRING_DATASOURCE_URL=jdbc:mysql://mysql-colis:3306/colis_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true \
    SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

ENTRYPOINT ["java", "-jar", "app.jar"]