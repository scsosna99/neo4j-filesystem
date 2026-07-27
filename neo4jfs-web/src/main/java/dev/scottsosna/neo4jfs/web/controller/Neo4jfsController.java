/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.web.controller;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.Map;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.NEO4J_URI_TEMPLATE;

/**
 * Base class for application's controllers
 */
public class Neo4jfsController {

    /**
     * Constructor.  Only available to child classes.
     */
    protected Neo4jfsController() {}

    /**
     * Create the base URI for a partition
     * @param partitionId identifies specific partition for the Neo4Jfs file system     * @return fully-qualified URI for the partition
     */
    protected URI uri(final String partitionId) {
        return URI.create(NEO4J_URI_TEMPLATE.formatted(partitionId));
    }

    /**
     * Check whether Neo4Jfs file system for this partition exists, if not create it.
     * @param partitionId identifies specific partition for the Neo4Jfs file system
     * @return file system for the partition
     * @throws IOException when file system cannot be created for whatever reason
     */
    protected FileSystem getFileSystem(final String partitionId) throws IOException {
        URI fsUri = uri(partitionId);
        try {
            return FileSystems.getFileSystem(fsUri);
        } catch (FileSystemNotFoundException e) {
            return FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of());
        }
    }
}
