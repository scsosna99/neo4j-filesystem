/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.exception;

import java.io.IOException;

/**
 * Base Neo4Jfs exception to handle when Java NIO doesn't have something appropriate.
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
