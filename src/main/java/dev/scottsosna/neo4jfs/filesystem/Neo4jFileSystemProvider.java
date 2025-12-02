package dev.scottsosna.neo4jfs.filesystem;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.neo4jfs.service.DirectoryService;
import dev.scottsosna.neo4jfs.service.FileService;
import dev.scottsosna.neo4jfs.service.FileSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.parameters.P;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.function.Function;

public class Neo4jFileSystemProvider extends FileSystemProvider {

    private FileSystemService fileSystemService;
    private DirectoryService directoryService;
    private FileService fileService;

    /**
     * Map of all currently open/loaded file systems.
     */
    private final Map<URI, Neo4jFileSystem> fileSystems = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(Neo4jFileSystemProvider.class);

    public Neo4jFileSystemProvider() {
        logger.info("Neo4jFileSystemProvider instance created.");
    }

    /**
     * @return Scheme for Neo4Jfs file system, must be unique among all file system providers.
     */
    @Override
    public String getScheme() {
        return Neo4jfsConstants.NEO4JFS_URI_SCHEME;
    }

    /**
     * Creates a new Neo4Jfs file system.
     * @param uri URI reference
     * @param env A map of provider specific properties to configure the file system; may be empty
     *
     * @return newly generated file system
     * @throws IOException if the file system has been previously created.
     */
    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        fileSystemService = (FileSystemService) env.get(FileSystemService.class.getName());
        directoryService = (DirectoryService) env.get(DirectoryService.class.getName());
        fileService = (FileService) env.get(FileService.class.getName());

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
     * @param uri URI specifying Neo4Jfs partition (host) and path.
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
     * @param uri URI specifying Neo4Jfs partition (host) and path.
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

    /**
     * Create new directory for the path specified.
     * @param dir the directory to create
     * @param attrs an optional list of file attributes to set atomically when creating the directory
     * @throws IOException something bad happens when attempting create.
     */
    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        directoryService.mkdir(dir.toUri());
    }

    /**
     * Deletes the entry - directory or file - specified by path.
     * @param path the path to the file to delete
     *
     * @throws IOException thrown when delete fails, such as trying to delete non-empty directory.
     */
    @Override
    public void delete(Path path) throws IOException {
        directoryService.delete(path.toUri());
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {

    }

    /**
     * Moves a file or directory to a new location.
     * @param source the path to the file to move
     * @param target the path to the target file
     * @param options options specifying how the move should be done
     * @throws IOException problems moving file or directory.
     */
    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        directoryService.move(source.toUri(), target.toUri(), options);
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
     * Checks for existence of file/directory at specified path.
     * @param path the path to the file to test
     * @param options options indicating how symbolic links are handled
     *
     * @return
     */
    @Override
    public boolean exists(Path path, LinkOption... options) {
        return directoryService.exists(path.toUri());
    }

    /**
     * Creates an InputStream for reading specified file
     * @param path the path to the file to open
     * @param options options specifying how the file is opened
     * @return input stream for reading file
     * @throws IOException exceptions for a multitude of reasons, file not found, access rights, etc.
     */
    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        if (path instanceof Neo4jfsPath p) {
            return fileService.getInputStream(p.toUri());
        } else {
            return null;
        }
    }

    /**
     * Creates an OutputStream for writing specified file, creating the file if it does not exist.
     * @param path the path to the file to open or create
     * @param options options specifying how the file is opened
     * @return output stream for writing file
     * @throws IOException exceptions for a multitude of reasons, file not found, access rights, etc.
     */
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        if (path instanceof Neo4jfsPath p) {
            return fileService.getOutputStream(p.toUri());
        } else {
            return null;
        }
    }

    /**
     * Removes file system from registry, called by file system when it's marked for closing.
     * @param uri base URI of file system
     */
    void removeFileSystem(URI uri) {
        logger.info("Removing file system from registry: " + uri.toString());
        try {
            fileSystems.remove(uri);
        } catch (Exception e) {
            //  make best effort and move on
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
