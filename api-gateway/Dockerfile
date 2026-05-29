FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml ./pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml

RUN mvn dependency:go-offline -q -f api-gateway/pom.xml

COPY api-gateway/src ./api-gateway/src

RUN mvn clean package -DskipTests -q -f api-gateway/pom.xml

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
