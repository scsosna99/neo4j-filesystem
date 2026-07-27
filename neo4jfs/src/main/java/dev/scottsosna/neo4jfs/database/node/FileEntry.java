/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
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
