package dev.scottsosna.sandbox.neo4jfs.database.repository;

import dev.scottsosna.sandbox.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.sandbox.neo4jfs.database.node.DirectoryEntry;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public interface DirectoryEntryRepository extends BaseEntryRepository {

    DirectoryEntry createRoot(URI uri);
    DirectoryEntry create(URI uri, String name);
    DirectoryEntry getParentWithChildren(URI uri, String parentId, int skip, int limit);
    DirectoryEntry findRoot(URI uri);
    List<BaseEntry> find(URI uri, Path path);
    List<BaseEntry> find(URI uri);
    BaseEntry parent(URI uri);
    boolean delete(URI uri, String fileNodeId);
    DirectoryEntry save(URI uri, DirectoryEntry d);
}
