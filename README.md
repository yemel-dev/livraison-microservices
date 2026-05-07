# 🚀 Plateforme de Suivi de Livraisons

Architecture Microservices · Event-Driven · Zero Trust · Kubernetes  
**Projet Middleware — Master 1 Informatique**

## Architecture

| Service | Port | Rôle |
|---|---|---|
| API Gateway | 8080 | Point d'entrée unique, validation JWT, routage |
| User Service | 8081 | Inscription, connexion, génération JWT |
| Colis Service | 8082 | CRUD colis, cycle de vie, Kafka Producer |
| Livreur Service | 8083 | Tournées, livraisons, Kafka Producer |
| Notification Service | 8084 | Kafka Consumer, simulation emails |

## Démarrage rapide (développement)

### 1. Démarrer l'infrastructure
```bash
cd docker
docker-compose up -d
```

### 2. Vérifier que tout tourne
```bash
docker-compose ps
```

### 3. Compiler le projet
```bash
cd ..
mvn clean install -DskipTests
```

### 4. Démarrer un service
```bash
cd user-service
mvn spring-boot:run
```

## Technologies
Spring Boot 3.2 · Spring Cloud Gateway · Apache Kafka · MySQL 8  
Docker · Kubernetes · JWT (HMAC-SHA256) · Lombok · Maven multi-modules