package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;

import java.io.IOException;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitor;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.List;

public interface DirectoryService {
    String SEPARATOR = "/";

    DirectoryEntry mkdir (URI uri) throws IOException;
    void copy(URI fromUri, URI toUri, CopyOption... options) throws IOException;
    void move(URI fromUri, URI toParentUri, CopyOption... options) throws IOException;
    void delete(URI uri) throws IOException;
    void rmdir(URI uri) throws IOException;
    void rmdirRecursively(URI uri) throws IOException;
    boolean exists(URI uri);

    DirectoryEntry addFile(URI uri, DirectoryEntry parent, FileEntry file);

    List<BaseEntry> find(URI uri);
    DirectoryEntry createRoot (URI uri);
    DirectoryEntry findOrCreateRoot(URI uri);
    BaseEntry parent(URI uri);
    DirectoryEntry findChildren(URI uri, String parentId, int skip, int limit);

    BasicFileAttributeView readAttributeView(URI uri, LinkOption... options) throws IOException;
    void setAttribute(URI uri, String viewName, String attribute, Object value, LinkOption... options) throws IOException;
    void dumpTree(URI uri) throws IOException;

    void registerFileService(FileService fs);
    void registerVisitor(final String key, final FileVisitor visitor);
}
