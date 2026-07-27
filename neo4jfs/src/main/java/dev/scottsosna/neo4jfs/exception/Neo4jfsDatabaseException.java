/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.exception;

/**
 * Neo4Jfs exception indicating database problems of some sort.
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
