package dev.scottsosna.neo4jfs.exception;

/**
 * Source and target URIs are identical when moving or copy.
 */
public class Neo4jfsIdenticalSourceTargetException extends Neo4jfsException{

    public Neo4jfsIdenticalSourceTargetException(String message) {
        super(message);
    }
}
