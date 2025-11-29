package dev.scottsosna.sandbox.neo4jfs.filesystem;

import dev.scottsosna.sandbox.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.sandbox.neo4jfs.service.DirectoryService;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

public class Neo4jFileSystem extends FileSystem {

    private boolean isOpen = false;

    /**
     * Environment-specifics passed to file system provider when creating file system.
     */
    private final Map<String,?> env;

    /**
     * Root Neo4jfs path for root for this file system.
     */
    private final Path rootPath;

    /**
     * Creating file system provider
     */
    private final Neo4jFileSystemProvider provider;

    /**
     * Base file system URI in the form of "neo4jfs://partition/"
     */
    @Getter
    private final URI uri;

    /**
     * Constructor
     * @param provider creator of file system
     * @param uri base URI for Neo4Jfs
     * @param env any specific environment properties required/interpreted by provider
     */
    public Neo4jFileSystem(final Neo4jFileSystemProvider provider, URI uri, Map<String, ?> env) {
        this.provider = provider;
        this.uri = uri;
        this.env = (env != null) ? env : Map.of();
        isOpen = true;
        rootPath = new Neo4jfsPath(this, Neo4jfsConstants.NAME_ROOT_DIRECTORY);
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    /**
     * Mark file system as closed and remove from provider registry.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (!isOpen) return;
        isOpen = false;
        provider.removeFileSystem(uri);
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return DirectoryService.SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return null;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return null;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Set.of();
    }

    @Override
    public Path getPath(String first, String... more) {
        return null;
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return null;
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return null;
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return null;
    }

    Path getRootPath() {
        return rootPath;
    }
}
