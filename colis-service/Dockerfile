# ─── Stage 1 : Build ─────────────────────────────────────────────────────────
# On utilise Maven + JDK pour compiler et packager le JAR
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copier le pom.xml en premier pour profiter du cache Docker :
# si seul le code change (pas les dépendances), Maven ne retélécharge rien
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copier le code source et compiler
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─── Stage 2 : Runtime ───────────────────────────────────────────────────────
# Image finale légère : JRE uniquement (pas de Maven, pas de code source)
# eclipse-temurin:21-jre-alpine ≈ 200MB vs ≈ 700MB pour le stage builder
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Récupérer uniquement le JAR compilé depuis le stage builder
COPY --from=builder /app/target/*.jar app.jar

# Port exposé par le service
EXPOSE 8082

# Variables d'environnement avec valeurs par défaut (surchargées par docker-compose/K8s)
ENV DB_USERNAME=root
ENV DB_PASSWORD=root
ENV SPRING_DATASOURCE_URL=jdbc:mysql://mysql-colis:3306/colis_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
ENV SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Démarrer le service
ENTRYPOINT ["java", "-jar", "app.jar"]