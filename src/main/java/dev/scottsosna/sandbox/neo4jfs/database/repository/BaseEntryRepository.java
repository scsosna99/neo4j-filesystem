package dev.scottsosna.sandbox.neo4jfs.database.repository;

import dev.scottsosna.sandbox.neo4jfs.database.node.BaseEntry;

import java.net.URI;

public interface BaseEntryRepository {
    BaseEntry findNamedChild(URI uri, String parentId, String name);
}
