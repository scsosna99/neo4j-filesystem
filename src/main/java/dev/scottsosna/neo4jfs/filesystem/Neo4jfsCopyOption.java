package dev.scottsosna.neo4jfs.filesystem;

import java.nio.file.CopyOption;

/**
 * Copy options specific to Neo4Jfs
 */
public enum Neo4jfsCopyOption implements CopyOption {
    /**
     * Deep directory copy.
     */
    RECURVSIVE_COPY
}
