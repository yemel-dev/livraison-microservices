package com.livraison.colis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du microservice colis-service.
 * Port : 8082
 */
@SpringBootApplication
public class ColisServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColisServiceApplication.class, args);
    }
}