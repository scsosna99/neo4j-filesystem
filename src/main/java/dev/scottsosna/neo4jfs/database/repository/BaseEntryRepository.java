package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;

import java.net.URI;

public interface BaseEntryRepository {
    BaseEntry findNamedChild(URI uri, String parentId, String name);
    void deleteRelationship(final URI uri, final String startId, final String endId);
    <T extends BaseEntry> void save(URI uri, T entry, Class<T> clazz);
}
