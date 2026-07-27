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
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

/**
 * Neo4J node representing a directory.
 */
@NodeEntity( label = "Directory")
@Getter @Setter @NoArgsConstructor
public class DirectoryEntry extends BaseEntry {

    /**
     * Flag: is the directory the root?
     */
    boolean root;

    /**
     * Subdirectories of this directory as defined by PARENT_OF relationship.
     */
    @Relationship(type = "PARENT_OF")
    List<DirectoryEntry> subdirs;

    /**
     * Files contained within this directory.
     */
    @Relationship(type = "CONTAINS")
    List<FileEntry> files;
}
