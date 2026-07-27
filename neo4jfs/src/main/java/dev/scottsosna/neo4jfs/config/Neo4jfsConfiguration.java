/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.config;

import dev.scottsosna.neo4jfs.security.AccessManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Neo4Jfs configuration class, values loaded from Spring application properties.
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class Neo4jfsConfiguration {

    private final AccessManager accessManager;

    /**
     * Constructore
     * @param accessManager configured access manager
     */
    public Neo4jfsConfiguration(final AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    /**
     * URI of the Neo4J database instance.
     */
    @Value("${neo4j.uri}")
    public String neo4jUri;

    /**
     * Authentication user name.
     */
    @Value("${neo4j.username}")
    public String neo4jUsername;

    /**
     * Authenticataion password.
     */
    @Value("${neo4j.password}")
    public String neo4jPassword;

    /**
     * The default Neo4J database to use for administrative tasks (e.g., creating new databases to support new partition).
     */
    @Value("${neo4j.database.default:system}")
    public String neo4jBaseDatabaseName;

    @Value("${neo4jfs.pageSize:500}")
    public Integer defaultPageSize;

    /**
     * Permissions applied when creating root directory for new Neo4Jfs file system, specific to the
     * {@code AccessManager} implementation configured.
     */
    public String rootPermissions;

    /**
     * Ensure the default permissions configured to apply to a root directory are valid.  Don't start if not.
     */
    @PostConstruct
    public void init() {
        rootPermissions = accessManager.rootPermissions();
        if (!accessManager.validatePermissions(rootPermissions)) {
            throw new IllegalArgumentException("Invalid permissions for root directory: " + rootPermissions);
        }
    }
}
