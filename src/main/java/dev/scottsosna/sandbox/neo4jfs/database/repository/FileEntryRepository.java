package dev.scottsosna.sandbox.neo4jfs.database.repository;

import dev.scottsosna.sandbox.neo4jfs.database.node.FileEntry;

import java.net.URI;

public interface FileEntryRepository extends BaseEntryRepository {

    FileEntry create(URI uri, String name, String storageId, long size);
    boolean delete(URI uri, String fileNodeId);
}
