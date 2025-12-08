package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.database.model.neo4j.Database;

import java.net.URI;
import java.util.List;

/**
 * Interface defining functionality for managing Neo4J databases.
 */
public interface DatabaseRepository {

    /**
     * Create new database based on URI
     *
     * @param fsUri Neo4Jfs file system URI for the database to be created
     * @return database just created
     */
    Database create(final URI fsUri);

    /**
     * Drop an existing database from the Neo4J instance.
     *
     * @param fsUri Neo4J file system URI for the database to be dropped
     */
    void drop(final URI fsUri);

    /**
     * Return a single database by, based on the "host" in the URI
     *
     * @param fsUri Neo4Jfs URI for database to be retrieved.
     * @return database found or null
     */
    Database find(final URI fsUri);

    /**
     * Return a single database by name
     *
     * @param dbName name of requested database
     * @return database found or null
     */
    Database find(final String dbName);

    /**
     * Return existing Neo4J databases
     *
     * @return list of daatabases currently in Neo4J instance.
     */
    List<Database> findAll();
}
