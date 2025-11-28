package dev.scottsosna.sandbox.neo4jfs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;

@SpringBootApplication
public class Neo4jfsApplication {

    /**
     * Main entry point for Spring Boot application
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Neo4jfsApplication.class, args);
    }
}
