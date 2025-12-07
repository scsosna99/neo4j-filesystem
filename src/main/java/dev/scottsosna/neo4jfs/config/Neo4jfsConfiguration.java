package dev.scottsosna.neo4jfs.config;

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

    @Value("${neo4j.uri}")
    public String neo4jUri;

    @Value("${neo4j.username}")
    public String neo4jUsername;

    @Value("${neo4j.password}")
    public String neo4jPassword;

    @Value("${neo4j.database.default:system}")
    public String neo4jBaseDatabaseName;

    @Value("${neo4jfs.pageSize:500}")
    public Integer defaultPageSize;
}
