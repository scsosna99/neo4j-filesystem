package dev.scottsosna.neo4jfs.filesystem.option;

import java.nio.file.CopyOption;

/**
 * Neo4Jfs-specific copy options
 */
public enum Neo4jfsCopyOption implements CopyOption {
    /**
     * Copying a directory should recurse and copy all subdirectories.
     */
    COPY_RECURSIVELY
}
