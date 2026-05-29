FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml ./pom.xml
COPY livreur-service/pom.xml ./livreur-service/pom.xml

RUN mvn dependency:go-offline -q -f livreur-service/pom.xml

COPY livreur-service/src ./livreur-service/src

RUN mvn clean package -DskipTests -q -f livreur-service/pom.xml

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/livreur-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
