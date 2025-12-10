package dev.scottsosna.neo4jfs.config;

import dev.scottsosna.neo4jfs.database.repository.util.PosixFilePermissionConverter;
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
     * Default permissions applied when creating the root directory for a Neo4Jfs partition.  When unconfigured,
     * it's read-write-execute for owner and read-only for group (640).
     */
    @Value("${neo4jfs.defaultRootPermissions:rwxr-x---}")
    public String defaultRootPermissions;

    /**
     * Ensure the default permissions configured to apply to a root directory are valid.  Don't start if not.
     */
    @PostConstruct
    public void init() {
        if (!PosixFilePermissionConverter.validate(defaultRootPermissions)) {
            throw new IllegalArgumentException("Invalid permissions for root directory: " + defaultRootPermissions);
        }
    }
}
