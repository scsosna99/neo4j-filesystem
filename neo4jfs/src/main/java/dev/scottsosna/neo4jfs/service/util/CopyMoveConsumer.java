package dev.scottsosna.neo4jfs.service.util;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.util.Consumer8;

import java.net.URI;
import java.nio.file.CopyOption;

/**
 * Functional intereface defining work methods used during copy or move operations, passed into a workflow method that
 * coordinates/validates/executes the copy or move.
 */
@FunctionalInterface
public interface CopyMoveConsumer extends Consumer8<
    URI,
    BaseEntry,
    DirectoryEntry,
    URI,
    BaseEntry,
    DirectoryEntry,
    String,
    CopyOption[]> {
}
