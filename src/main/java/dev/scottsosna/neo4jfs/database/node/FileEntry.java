package dev.scottsosna.neo4jfs.database.node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Neo4J node representing a file.
 */
@NodeEntity( label = "File")
@Getter @Setter @NoArgsConstructor
public class FileEntry extends BaseEntry {

    /**
     * Identifier to access files stored by a Storage Manager, format determined by Storage Manager implementation.
     */
    String storageId;

    /**
     * Size of file in bytes.
     */
    long size;
}
