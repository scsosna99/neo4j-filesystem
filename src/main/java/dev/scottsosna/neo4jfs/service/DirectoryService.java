package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;

import java.io.IOException;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitor;
import java.util.List;

public interface DirectoryService {
    String SEPARATOR = "/";

    boolean exists(URI uri);
    DirectoryEntry createRoot (URI uri);
    DirectoryEntry findOrCreateRoot(URI uri);
    DirectoryEntry mkdir (URI uri) throws IOException;
    void delete(URI uri) throws IOException;
    void move(URI fromUri, URI toParentUri, CopyOption... options) throws IOException;
    void rmdir(URI uri) throws IOException;
    void rmdirRecursively(URI uri) throws IOException;
    void dumpTree(URI uri) throws IOException;
    BaseEntry parent(URI uri);
    List<BaseEntry> find(URI uri);
    DirectoryEntry findChildren(URI uri, String parentId, int skip, int limit);
    DirectoryEntry addFile(URI uri, DirectoryEntry parent, FileEntry file);
    DirectoryEntry getRoot(URI uri);
    void registerFileService(FileService fs);
    void registerVisitor(final String key, final FileVisitor visitor);
}
