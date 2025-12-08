package dev.scottsosna.neo4jfs.database.model.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Basic info about file persisted by Storage Manager.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StorageFileInfo {

    /**
     * Implementation-specific identifier for file.
     */
    private String storageId;

    /**
     * Size of file in bytes.
     */
    private long size;
}
