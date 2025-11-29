package dev.scottsosna.sandbox.neo4jfs.filesystem;

import dev.scottsosna.sandbox.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.sandbox.neo4jfs.service.DirectoryService;
import dev.scottsosna.sandbox.neo4jfs.service.FileSystemService;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Neo4jFileSystemProvider extends FileSystemProvider {

    private FileSystemService fileSystemService;
    private DirectoryService directoryService;

    /**
     * Map of all currently open/loaded file systems.
     */
    private final Map<URI, Neo4jFileSystem> fileSystems = new ConcurrentHashMap<>();

    public Neo4jFileSystemProvider() {
        System.out.println("Neo4jFileSystemProvider constructor");
    }

    /**
     * @Return Scheme for Neo4jfs file system, must be unique among all file system providers.
     */
    @Override
    public String getScheme() {
        return Neo4jfsConstants.NEO4JFS_URI_SCHEME;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        fileSystemService = (FileSystemService) env.get(FileSystemService.class.getName());
        directoryService = (DirectoryService) env.get(DirectoryService.class.getName());

        //  Truncate URI to root path and check for existence.
        URI truncatedUri = truncateUri(uri);
        if (fileSystems.containsKey(uri)) {
            throw new FileSystemAlreadyExistsException(truncatedUri.toString());
        }

        //  Create new file system and add to map
        fileSystemService.init(truncatedUri);
        Neo4jFileSystem created = new Neo4jFileSystem(this, uri, env);
        fileSystems.put(truncatedUri, created);

        //  Return created file system.
        return created;
    }

    /**
     * Retrieves opened file system or throws exception if not found in map.
     * @param uri URI specifying Neo4jfs partition (host) and path.
     * @return Neo4Jfs file system.
     */
    @Override
    public FileSystem getFileSystem(URI uri) {
        FileSystem toReturn = fileSystems.get(truncateUri(uri));
        if (toReturn == null) {
            throw new FileSystemNotFoundException(uri.toString());
        } else {
            return toReturn;
        }
    }

    /**
     * Create Neo4Jfs path from URI.
     *
     * @param uri URI specifying Neo4jfs partition (host) and path.
     * @return Neo4Jfs path.
     */
    @Override
    public Path getPath(URI uri) {
        validateUri(uri);

        //  If no path specified, default to root directory.
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = Neo4jfsConstants.NAME_ROOT_DIRECTORY;
        }

        try {
            //  Get file system from URI.
            FileSystem fs = getFileSystem(uri);

            //  Path is specific to file system host (partition) and needs to be stored with created path.
            return new Neo4jfsPath(fs, path);
        } catch (FileSystemNotFoundException e) {
            //  The file system won't exist during FileSystems.newFileSystem(), chicken-and-egg problem,
            //  so get around problem by just using default path.
            return Path.of(path);
        }
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return null;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return null;
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

    }

    @Override
    public void delete(Path path) throws IOException {
        directoryService.delete(path.toUri());
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {

    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return false;
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {

    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return Map.of();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {

    }

    /**
     * Removes file system from registry, called by file system when it's marked for closing.
     * @param uri base URI of file system
     */
    void removeFileSystem(URI uri) {
        try {
            fileSystems.remove(uri);
        } catch (Exception e) {
            System.out.println("Error removing file system: " + uri);
        }
    }

    /**
     * The URI's path is meaningless and could cause for multiple instances of identical file system to be created,
     * so always truncate the path to the root directory.
     * @param uri
     * @return URI with no extra path.
     */
    private URI truncateUri(URI uri) {
        validateUri(uri);
        return uri.resolve(Neo4jfsConstants.NAME_ROOT_DIRECTORY);
    }

    /**
     * Validate all provided URIs to ensure completeness for Neo4Jfs
     * @param uri URI to validate.
     */
    private void validateUri(URI uri) {

        //  Each implemented file system has a unique scheme.
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals(getScheme())) {
            throw new IllegalArgumentException("URI scheme must be " + getScheme() + ".");
        }

        //  URI's host provides file system partition name.
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("URI host required as partition name.");
        }
    }
}
