/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.web.controller.request;

/**
 * Request for creating a temporary directory
 * @param path directory in which temp file is created
 * @param prefix optional prefix for temp directry name
 */
public record TempDirectoryRequest(String path, String prefix) {
}
