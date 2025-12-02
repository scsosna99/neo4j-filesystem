package dev.scottsosna.neo4jfs.exception;

import java.io.IOException;

/**
 * Base Neo4Jfs exception.
 */
public class Neo4jfsException extends IOException {
    public Neo4jfsException() {
        super();
    }

    public Neo4jfsException(String message) {
        super(message);
    }

    public Neo4jfsException(String message, Throwable cause) {
        super(message, cause);
    }

    public Neo4jfsException(Throwable cause) {
        super(cause);
    }
}
