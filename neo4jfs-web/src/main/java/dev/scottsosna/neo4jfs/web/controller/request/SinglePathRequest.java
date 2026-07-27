/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.web.controller.request;

/**
 * Request for any endpoint requiring a fully-qualified file or directory path.
 * @param path fully-qualified (not relative) Neo4Jfs path for a file or directory
 */
public record SinglePathRequest(String path) {
}
