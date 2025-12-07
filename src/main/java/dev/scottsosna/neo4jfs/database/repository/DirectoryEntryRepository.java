package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public interface DirectoryEntryRepository extends BaseEntryRepository {

    DirectoryEntry createRoot(URI uri);
    DirectoryEntry create(URI uri, String name);
    boolean pathExists(URI uri);
    DirectoryEntry getParentWithChildren(URI uri, String parentId, int skip, int limit);
    List<FileEntry> getFiles(final URI fsUri, final String parentId, final int skip, final int limit);
    List<DirectoryEntry> getSubdirs(final URI fsUri, final String parentId, final int skip, final int limit);
    DirectoryEntry findRoot(URI uri);
    List<BaseEntry> find(URI uri);
    List<BaseEntry> find(URI uri, Path path);
    List<BaseEntry> find(URI uri, Path path, boolean endNodeOptional);
    List<BaseEntry> findFile(URI uri, Path path, boolean endNodeOptional);
    BaseEntry parent(URI uri);
    boolean delete(URI uri, String fileNodeId);
    DirectoryEntry save(URI uri, DirectoryEntry d);
}
