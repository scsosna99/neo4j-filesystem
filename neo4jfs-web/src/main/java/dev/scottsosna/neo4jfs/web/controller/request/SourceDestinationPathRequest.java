/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.web.controller.request;

/**
 * Request for endpoints that require two Neo4Jfs paths, such as a move or copy operation.
 * @param sourcePath
 * @param destinationPath
 */
public record SourceDestinationPathRequest(String sourcePath, String destinationPath) {
}
