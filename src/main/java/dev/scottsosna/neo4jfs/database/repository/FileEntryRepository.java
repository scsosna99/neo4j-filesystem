package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.database.node.FileEntry;

import java.net.URI;

/**
 * Interface for managing {@link FileEntry} nodes.
 */
public interface FileEntryRepository extends BaseEntryRepository {

    /**
     * Create a new file entry
     * @param fsUri Neo4Jfs base URI
     * @param name file name
     * @param storageId ID for file as stored in Storage Manager
     * @param size size of the file.
     * @return persisted FileEntry
     */
    FileEntry create(final URI fsUri, final String name, final String storageId, long size);

    /**
     * Delete a file entry by its Neo4J node ID
     * @param fsUri Neo4Jfs base URI
     * @param fileNodeId Neo4J node ID of the file entry to delete
     * @return true if deleted, false otherwise.
     */
    boolean delete(final URI fsUri, final String fileNodeId);

    /**
     * Load the specific file by its Neo4J node ID.
     * @param fsUri Neo4J base URI
     * @param fileNodeId Neo4J node ID of the file to load
     * @return FileEntry returned from database
     */
    FileEntry load(final URI fsUri, final String fileNodeId);
}
