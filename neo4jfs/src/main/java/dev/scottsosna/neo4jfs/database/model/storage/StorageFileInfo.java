/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
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
