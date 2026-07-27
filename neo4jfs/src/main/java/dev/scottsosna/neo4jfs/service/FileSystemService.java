/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;

/**
 * Interface for managing Neo4Jfs file system.
 */
public interface FileSystemService {
    /**
     * Creates new or validates existing file system, ensures root "directory" (node) exists and that
     * storage partition is available/usable.
     * @param fsUri Neo4Jfs URI for the file system to initialize.
     */
    void init(final URI fsUri) throws IOException;

    /**
     * Deletes the complete file system, including content managed by Storage Manager.
     * @param fsUri Neo4Jfs URI for the file system to delete.
     */
    void drop(final URI fsUri) throws IOException;

    /**
     * The file system's {@code FileStore} instance is based on Storage Manager's partition
     * @param fsUri Neo4Jfs URI for the file system.
     * @return {@code FileStore} for the partition.
     * @throws IOException if an I/O error occurs.
     */
    FileStore getFileStore(final URI fsUri) throws IOException;
}