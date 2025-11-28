package dev.scottsosna.sandbox.neo4jfs.database.model.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StorageFileInfo {
    private String storageId;
    private long size;
}
