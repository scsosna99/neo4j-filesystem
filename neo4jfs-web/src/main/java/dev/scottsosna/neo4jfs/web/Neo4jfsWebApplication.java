/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
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
