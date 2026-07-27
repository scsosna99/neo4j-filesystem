/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.exception;

/**
 * Source and target URIs are identical when moving or copy.
 */
public class Neo4jfsIdenticalSourceTargetException extends Neo4jfsException{

    public Neo4jfsIdenticalSourceTargetException(String message) {
        super(message);
    }
}
