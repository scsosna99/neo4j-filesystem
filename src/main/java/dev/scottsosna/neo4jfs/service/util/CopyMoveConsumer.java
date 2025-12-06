package dev.scottsosna.neo4jfs.service.util;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.util.Consumer7;

import java.net.URI;
import java.nio.file.CopyOption;
import java.util.Set;

@FunctionalInterface
public interface CopyMoveConsumer extends Consumer7<
    URI,
    BaseEntry,
    DirectoryEntry,
    BaseEntry,
    DirectoryEntry,
    String,
    CopyOption[]> {
}
