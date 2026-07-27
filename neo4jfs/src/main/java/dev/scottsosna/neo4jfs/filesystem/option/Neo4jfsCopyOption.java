/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
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
