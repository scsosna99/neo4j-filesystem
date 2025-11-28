package dev.scottsosna.sandbox.neo4jfs.database.repository;

import dev.scottsosna.sandbox.neo4jfs.database.model.neo4j.Database;
import dev.scottsosna.sandbox.neo4jfs.database.node.DirectoryEntry;

import java.net.URI;
import java.util.List;

public interface DatabaseRepository {

    Database create(URI uri);
    void drop(URI uri);
    Database find(URI uri);
    Database find(String dbName);
    List<Database> findAll();
}
