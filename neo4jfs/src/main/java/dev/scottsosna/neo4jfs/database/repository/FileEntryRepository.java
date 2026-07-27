/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.database.node.FileEntry;

import java.net.URI;
import java.util.List;

/**
 * Interface for managing {@link FileEntry} nodes.
 */
public interface FileEntryRepository extends BaseEntryRepository {

    /**
     * Persist file entry.
     * @param fsUri Neo4Jfs base URI
     * @param f file entry to persist
     * @return updated file entry
     */
    FileEntry create(final URI fsUri, final FileEntry f);

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

    /**
     * Find files associated with specified external storage ID
     * @param fsUri Neo4Jfs base URI
     * @param storageId external storage ID
     * @return List of files associated with storage ID
     */
    List<FileEntry> findByStorageId(final URI fsUri, final String storageId);
}
