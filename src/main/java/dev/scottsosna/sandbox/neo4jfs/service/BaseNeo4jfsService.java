package dev.scottsosna.sandbox.neo4jfs.service;

import java.net.URI;

import static dev.scottsosna.sandbox.neo4jfs.config.Neo4jfsConstants.NEO4JFS_URI_SCHEME;

public class BaseNeo4jfsService {

    /**
     * Protected constructor, should never be a standalone instance.
     */
    protected BaseNeo4jfsService() {}

    /**
     * Confirm URI has correct scheme.
     * @param uri URI to validate.
     */
    protected void checkSchema(URI uri) {
        if (!NEO4JFS_URI_SCHEME.equals(uri.getScheme())) {
            throw new IllegalArgumentException("URI scheme must be " + NEO4JFS_URI_SCHEME + ".");
        }
    }
}
