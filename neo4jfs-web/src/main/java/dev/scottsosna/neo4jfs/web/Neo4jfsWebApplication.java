package dev.scottsosna.neo4jfs.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Neo4Jfs application class
 */
@SpringBootApplication(scanBasePackages = {"dev.scottsosna.neo4jfs","dev.scottsosna.neo4jfs.web"})
public class Neo4jfsWebApplication {

    /**
     * Main entry point for Spring Boot application
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Neo4jfsWebApplication.class, args);
    }
}
