package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
import dev.scottsosna.neo4jfs.database.model.neo4j.Database;
import org.neo4j.ogm.model.Result;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Component
public class DatabaseRepositoryImpl extends BaseEntryRepositoryImpl implements DatabaseRepository {

    public DatabaseRepositoryImpl(Neo4jfsConfiguration config) {
        super(config);
    }

    /**
     * Create new database based on URI
     * @param uri base URI of the file system
     * @return database just created
     */
    @Override
    public Database create(URI uri) {
        String dbName = uri.getHost();
        query("CREATE DATABASE $database", Map.of("database", dbName));
        return find(dbName);
    }

    /**
     * Drop an existing database from the Neo4J instance.
     * @param uri base URI of the file system
     */
    public void drop(URI uri) {
        query("DROP DATABASE $database", Map.of("database", uri.getHost()));
    }

    /**
     * Return a single database by, based on the "host" in the URI
     * @param uri URI for the Neo4J filesystem
     * @return database found or null
     */
    @Override
    public Database find(URI uri) {
        return find(uri.getHost());
    }

    /**
     * Return a single database by name
     * @param dbName name of requested database
     * @return database found or null
     */
    @Override
    public Database find(String dbName) {
        Result r = query("SHOW DATABASE $database", Map.of("database", dbName));
        List<Database> dbs = deserialize(r);
        return (dbs.isEmpty() ? null : dbs.getFirst());
    }

    /**
     * Return all databases
     * @return list of daatabases currently in Neo4J instance.
     */
    @Override
    public List<Database> findAll() {
        Result r = query("SHOW DATABASES");
        return deserialize(r);
    }


    /**
     * Take results of query and deserialize into Database objects.
     * @param r Query results
     * @return list of zero or more Database objects
     */
    private List<Database> deserialize(Result r) {
        return StreamSupport.stream(r.spliterator(), false)
            .map(Database::new).
            toList();
    }
}
