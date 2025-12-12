package dev.scottsosna.neo4jfs.web.controller;

import java.net.URI;

public class Neo4jfsController {

    static final private String NEO4J_URI_TEMPLATE = "neo4jfs://%s/";
    /**
     * Constructor.  Only available to child classes.
     */
    protected Neo4jfsController() {}

    protected URI uri(final String partitionId) {
        return URI.create(NEO4J_URI_TEMPLATE.formatted(partitionId));
    }
}
