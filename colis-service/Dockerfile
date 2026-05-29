FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml ./pom.xml
COPY colis-service/pom.xml ./colis-service/pom.xml

RUN mvn dependency:go-offline -q -f colis-service/pom.xml

COPY colis-service/src ./colis-service/src

RUN mvn clean package -DskipTests -q -f colis-service/pom.xml

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/colis-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
