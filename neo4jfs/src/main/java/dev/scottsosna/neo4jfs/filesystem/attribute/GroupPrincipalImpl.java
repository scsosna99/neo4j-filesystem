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
