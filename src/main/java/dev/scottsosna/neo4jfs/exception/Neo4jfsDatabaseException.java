package dev.scottsosna.neo4jfs.exception;

/**
 * Neo4Jfs exception signalling database problems.
 */
public class Neo4jfsDatabaseException extends Neo4jfsException {
    public Neo4jfsDatabaseException() {
        super();
    }

    public Neo4jfsDatabaseException(String message) {
        super(message);
    }

    public Neo4jfsDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public Neo4jfsDatabaseException(Throwable cause) {
        super(cause);
    }
}
