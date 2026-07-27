/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.exception;

/**
 * Expected/requested Storage Manager partition does not exist.
 */
public class Neo4jfsNoSuchPartition extends Neo4jfsException {

    /**
     * Constructor
     * @param partitionName requested partition name
     */
    public Neo4jfsNoSuchPartition(String partitionName) {
        super(partitionName);
    }
}
