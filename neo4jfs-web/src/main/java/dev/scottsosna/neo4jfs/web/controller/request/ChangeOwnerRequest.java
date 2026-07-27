/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.web.controller.request;

/**
 * Request to change the owner of a file or directory.
 * @param path fully-qualified (not relative) path to the file or directory
 * @param owner username of the new owner
 */
public record ChangeOwnerRequest(String path, String owner) {
}
