package dev.scottsosna.neo4jfs.filesystem;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

/**
 * Neo4Jfs file system implementation.
 */
public class Neo4jFileSystem extends FileSystem {

    private boolean isOpen = false;

    /**
     * Environment-specifics passed to file system provider when creating file system.
     */
    private final Map<String,?> env;

    /**
     * Root Neo4Jfs path for root for this file system.
     */
    private final Path rootPath;

    /**
     * Creating file system provider
     */
    private final Neo4jFileSystemProvider provider;

    /**
     * Pre-created set of root paths for this file system.
     */
    private final Set<Path> rootPaths;

    /**
     * Base file system URI in the form of "neo4jfs://partition/"
     */
    @Getter
    private final URI uri;

    /**
     * Constructor
     * @param provider creator of file system
     * @param fsUri Neo4Jfs URI for this file system
     * @param env any specific environment properties required/interpreted by provider
     */
    public Neo4jFileSystem(final Neo4jFileSystemProvider provider,
                           final URI fsUri,
                           final Map<String, ?> env) {
        this.provider = provider;
        this.uri = fsUri;
        this.env = (env != null) ? env : Map.of();
        isOpen = true;
        rootPath = new Neo4jfsPath(this, Neo4jfsConstants.NAME_ROOT_DIRECTORY);
        rootPaths = Set.of(rootPath);
    }

    /**
     * @return the supporting provider for this file system/partition instance.
     */
    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    /**
     * MClose file system and remove from provider registry.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (!isOpen) return;
        isOpen = false;
        provider.removeFileSystem(uri);
    }

    /**
     * @return true if file system is open and usable, false otherwise.
     */
    @Override
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Neo4Jfs file systems/partitions are always read/write.
     * @return false
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * @return the path separator for his file system
     */
    @Override
    public String getSeparator() {
        return Neo4jfsConstants.PATH_SEPARATOR;
    }

    /**
     * @return set of root paths. Neo4Jfs has only one root path (e.g., no mountable volumes).
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        return rootPaths;
    }

    /**
     * TODO: need to figure out way of getting file store from storage manager.
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        return null;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Set.of();
    }

    /**
     * Creates Neo4Jfs-specific path from the path string(s) passed in
     * @param first the path string or initial part of the path string
     * @param more additional strings to be joined to form the path string
     * @return new Neo4Jfs path
     */
    @Override
    public Path getPath(String first, String... more) {
        return new Neo4jfsPath(this, Path.of(first, more).toString());
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

    /**
     * @return root path for this file system
     */
    Path getRootPath() {
        return rootPath;
    }
}
