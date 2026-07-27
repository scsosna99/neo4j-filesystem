/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.filesystem.attribute;

import java.nio.file.attribute.GroupPrincipal;

/**
 * Simple GroupPrincipal implementation.
 * @param name group name
 */
public record GroupPrincipalImpl (String name) implements GroupPrincipal {

    /**
     * @return the owning group for this file/directory
     */
    @Override
    public String getName() {
        return name;
    }
}
