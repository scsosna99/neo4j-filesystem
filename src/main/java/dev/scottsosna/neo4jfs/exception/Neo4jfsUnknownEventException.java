package dev.scottsosna.neo4jfs.exception;

/**
 * Neo4Jfs exception indicating an unknown/unexpected event type encountered.
 */
public class Neo4jfsUnknownEventException extends Neo4jfsException {
    public Neo4jfsUnknownEventException() {
        super();
    }

    public Neo4jfsUnknownEventException(String message) {
        super(message);
    }

    public Neo4jfsUnknownEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public Neo4jfsUnknownEventException(Throwable cause) {
        super(cause);
    }
}
