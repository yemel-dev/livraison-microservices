package com.livraison.notification;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class NotificationServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    // CommandLineRunner s'exécute APRÈS que Spring a tout démarré
    // Le @Bean dit à Spring de gérer cette méthode
    @Bean
    public CommandLineRunner demarrage() {
        return args -> {
            log.info("================================================");
            log.info("🚀 Notification Service démarré !");
            log.info("📡 En écoute sur les topics Kafka :");
            log.info("   → colis.created");
            log.info("   → colis.status_changed");
            log.info("   → livraison.done");
            log.info("================================================");
        };
    }
}