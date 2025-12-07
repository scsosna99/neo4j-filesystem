package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
import dev.scottsosna.neo4jfs.database.model.neo4j.Database;
import org.neo4j.ogm.model.Result;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * Functionality for managing Neo4J databases.  Note that "databases" are not graph objects, so
 * commands not Cypher queries are used.
 */
@Component
public class DatabaseRepositoryImpl extends BaseEntryRepositoryImpl implements DatabaseRepository {

    /**
     * Neo4J commands for working with databases.
     */
    private static final String CREATE_DATABASE = "CREATE DATABASE $database";
    private static final String DROP_DATABASE = "DROP DATABASE $database";
    private static final String SHOW_DATABASE = "SHOW DATABASE $database";
    private static final String SHOW_DATABASES = "SHOW DATABASES";
    private static final String PARAMETER_DATABASE = "database";

    /**
     * Constructor
     * @param config configuration bean hold Neo4J connection and authentication credentials.
     */
    public DatabaseRepositoryImpl(final Neo4jfsConfiguration config) {
        super(config);
    }

    /**
     * Create new database based on URI
     *
     * @param fsUri Neo4Jfs file system URI for the database to be created
     * @return database just created
     */
    @Override
    public Database create(final URI fsUri) {
        String dbName = fsUri.getHost();
        query(CREATE_DATABASE, Map.of(PARAMETER_DATABASE, dbName));
        return find(dbName);
    }

    /**
     * Drop an existing database from the Neo4J instance.
     *
     * @param fsUri Neo4J file system URI for the database to be dropped
     */
    public void drop(final URI fsUri) {
        query(DROP_DATABASE, Map.of(PARAMETER_DATABASE, fsUri.getHost()));
    }

    /**
     * Return a single database by, based on the "host" in the URI
     *
     * @param fsUri Neo4Jfs URI for database to be retrieved.
     * @return database found or null
     */
    @Override
    public Database find(final URI fsUri) {
        return find(fsUri.getHost());
    }

    /**
     * Return a single database by name
     *
     * @param dbName name of requested database
     * @return database found or null
     */
    @Override
    public Database find(final String dbName) {
        Result r = query(SHOW_DATABASE, Map.of(PARAMETER_DATABASE, dbName));
        List<Database> dbs = deserialize(r);
        return (dbs.isEmpty() ? null : dbs.getFirst());
    }

    /**
     * Return existing Neo4J databases
     *
     * @return list of daatabases currently in Neo4J instance.
     */
    @Override
    public List<Database> findAll() {
        Result r = query(SHOW_DATABASES);
        return deserialize(r);
    }

    /**
     * Take results of query and deserialize into Database objects.
     *
     * @param r Query results
     * @return list of zero or more Database objects
     */
    private List<Database> deserialize(final Result r) {
        return StreamSupport.stream(r.spliterator(), false)
            .map(Database::new).
            toList();
    }
}
