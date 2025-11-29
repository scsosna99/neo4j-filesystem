package dev.scottsosna.sandbox.neo4jfs.service;

import java.net.URI;

import static dev.scottsosna.sandbox.neo4jfs.config.Neo4jfsConstants.NEO4JFS_URI_SCHEME;

public class BaseNeo4jfsService {

    /**
     * Protected constructor, should never be a standalone instance.
     */
    protected BaseNeo4jfsService() {}

    /**
     * Confirm URI is correctly formatted.
     * @param uri URI to validate.
     */
    protected void checkUri(URI uri) {
        if (!NEO4JFS_URI_SCHEME.equals(uri.getScheme())) {
            throw new IllegalArgumentException("URI scheme must be " + NEO4JFS_URI_SCHEME + ".");
        }

        //  Empty path not allowed, must at least provide root "/"
        if (uri.getPath() == null || uri.getPath().isEmpty()) {
            throw new IllegalArgumentException("URI path required.");
        }
    }
}
