package dev.scottsosna.sandbox.neo4jfs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableSpringConfigured
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class Neo4jfsConfiguration {

    @Value("${neo4j.uri}")
    public String neo4jUri;

    @Value("${neo4j.username}")
    public String neo4jUsername;

    @Value("${neo4j.password}")
    public String neo4jPassword;

    @Value("${neo4j.database.default:system}")
    public String neo4jBaseDatabaseName;

}
