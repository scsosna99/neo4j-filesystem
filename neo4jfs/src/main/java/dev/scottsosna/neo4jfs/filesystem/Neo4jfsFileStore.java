/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.filesystem;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;

import java.nio.file.FileStore;

/**
 * Neo4Jfs implementation of {@link FileStore}.  The files are persisted/stored in the specific Storage Mnaager instance
 * from which the details will be derived.  This is just the abstract class with the Neo4Jfs specifics
 */
abstract public class Neo4jfsFileStore extends FileStore {

    /**
     * @return the name of the file store.
     */
    @Override
    public String name() {
        return "";
    }

    /**
     * @return the file store type, which is "neo4jfs".
     */
    @Override
    public String type() {
        return Neo4jfsConstants.NEO4JFS_URI_SCHEME;
    }

    /**
     * By default, Neo4Jfs file stores are always read/write.
     * @return false
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }
}
