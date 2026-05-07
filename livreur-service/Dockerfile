# ÉTAPE 1 : on utilise une image Maven pour compiler le code
# "builder" est juste un nom qu'on donne à cette étape
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# On copie d'abord le pom.xml pour profiter du cache Docker
# (si le code change mais pas les dépendances, Maven ne les retélécharge pas)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Ensuite on copie le code source et on compile
COPY src ./src
RUN mvn clean package -DskipTests -q

# ÉTAPE 2 : image finale légère, sans Maven ni code source
# On prend juste le JRE (Java Runtime) pour exécuter le JAR
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# On récupère uniquement le JAR compilé depuis l'étape builder
COPY --from=builder /app/target/*.jar app.jar

# Le port que ce service expose
EXPOSE 8083

# La commande qui démarre le service
ENTRYPOINT ["java", "-jar", "app.jar"]