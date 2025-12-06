package dev.scottsosna.neo4jfs.service;

import java.net.URI;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.NEO4JFS_URI_SCHEME;

/**
 * Base class for Neo4fjs services.
 */
public class BaseNeo4jfsService {

    /**
     * Constructor protected to prevent direct instantiation.
     */
    protected BaseNeo4jfsService() {}

    /**
     * Check URI for structure and usability.
     *
     * @param uri URI to validate.
     * @return normalized URI.
     */
    protected URI checkUri(URI uri) {
        if (!NEO4JFS_URI_SCHEME.equals(uri.getScheme())) {
            throw new IllegalArgumentException("URI scheme must be " + NEO4JFS_URI_SCHEME + ".");
        }

        //  Empty path not allowed, must at least provide root "/"
        if (uri.getPath() == null || uri.getPath().isEmpty()) {
            throw new IllegalArgumentException("URI path required.");
        }

        //  Normalize URI in case anyone doing tricky navigation.
        return uri.normalize();
    }
}
