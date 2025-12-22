package dev.scottsosna.neo4jfs.filesystem;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.filesystem.attribute.BasicFileAttributesImpl;
import dev.scottsosna.neo4jfs.service.DirectoryService;
import dev.scottsosna.neo4jfs.service.FileService;
import dev.scottsosna.neo4jfs.service.FileSystemService;
import dev.scottsosna.neo4jfs.util.SpringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.*;

/**
 * Neo4Jfs file system provider as defined by the Java NIO.2 API.  Implementing a file provider allows
 * {@code java.nio.file.File} to work directly with Neo4Jfs just as any other file system, such as disk.
 */
public class Neo4jFileSystemProvider extends FileSystemProvider {

    /**
     * Spring bean for file system service, responsible for setting up/tearing down file system.
     */
    final private FileSystemService fileSystemService;

    /**
     * Spring bean for directory service, responsible for Neo4Jfs directory operations.
     */
    final private DirectoryService directoryService;

    /**
     * Spring bean for file service, responsible for Neo4Jfs file operations.
     */
    final private FileService fileService;

    /**
     * Map of all currently open/loaded file systems.
     */
    private final Map<URI, Neo4jFileSystem> fileSystems = new ConcurrentHashMap<>();

    /**
     * Logger for this class.
     */
    private final static Logger logger = LoggerFactory.getLogger(Neo4jFileSystemProvider.class);

    /**
     * Constructor
     */
    public Neo4jFileSystemProvider() {
        //  File system providers are instantiated by the JDK and not by Spring, yet we need the Spring beans to
        //  do the work.  The {@code SpringContext) utility retrieves the beans from the Spring context.
        fileSystemService = SpringContext.getBean(FileSystemService.class);
        directoryService = SpringContext.getBean(DirectoryService.class);
        fileService = SpringContext.getBean(FileService.class);
        logger.debug("Neo4jFileSystemProvider instance created.");
    }

    /**
     * @return Scheme for Neo4Jfs is, unsurprisingly, "neo4jfs".  Each file system's scheme must be unique.
     */
    @Override
    public String getScheme() {
        return Neo4jfsConstants.NEO4JFS_URI_SCHEME;
    }

    /**
     * Creates a new Neo4Jfs file system.
     *
     * @param uri URI reference
     * @param env A map of provider specific properties to configure the file system; may be empty
     * @return newly generated file system
     * @throws IOException if the file system has been previously created.
     */
    @Override
    public FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        validateUri(uri);

        //  The base file system URI is "neo4jfs://<partition>/", anything else is extraneous, so only
        //  keep the bare minimum by truncating.
        URI truncatedUri = truncateUri(uri);
        if (fileSystems.containsKey(uri)) {
            logger.debug("File system {} already exists, throwing exception", truncatedUri);
            throw new FileSystemAlreadyExistsException(truncatedUri.toString());
        }

        //  From a JDK perspective, a new file system is created and integrated into the available file systems
        //  for the running application.  However, the underpinnings may already exist - the partition-specific
        //  database and storage, similar to an already-existing disk.
        fileSystemService.init(truncatedUri);
        Neo4jFileSystem created = new Neo4jFileSystem(this, uri, env);
        fileSystems.put(truncatedUri, created);

        //  Return created file system.
        return created;
    }

    /**
     * Retrieves an already-created and registered Neo4Jfs file system.
     *
     * @param uri URI specifying the specific file system to retrieve
     * @return Neo4Jfs file system.
     * @throws FileSystemNotFoundException if the file system has not been created yet.
     */
    @Override
    public FileSystem getFileSystem(final URI uri) {
        validateUri(uri);

        //  Attempt to retrieve registered file system based on truncated URI.
        URI fileSystemUri = truncateUri(uri);
        FileSystem toReturn = fileSystems.get(fileSystemUri);
        if (toReturn != null) {
            return toReturn;
        }

        //  Requested file system has not been created/registered yet.
        logger.warn("File system {} not found in registry", fileSystemUri);
        throw new FileSystemNotFoundException(fileSystemUri.toString());
    }

    /**
     * Create fully-qualified Neo4Jfs path from URI.
     *
     * @param uri URI of fully-qualified Neo4Jfs path to file or directory.
     * @return Neo4Jfs path.
     */
    @Override
    public Path getPath(final URI uri) {
        validateUri(uri);

        //  No path specified - either "neo4jfs://<partition>" or "neo4jfs://<partition>/" - defaults to root.
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
            //  work-around is to create normal file system path without Neo4Jfs scheme or partition (host).
            logger.warn("File system {} not found during creation", uri.toString());
            return Path.of(path);
        }
    }

    /**
     * Opens or creates a file, returning a seekable byte channel to access the file. This method works in exactly
     * the manner specified by the Files#newByteChannel(Path,Set,FileAttribute[]) method.
     *
     * @param path the path to the file to open or create
     * @param options options specifying how the file is opened
     * @param attrs an optional list of file attributes to set atomically when creating the file
     * @return seekable byte channel for reading/writing to underlying Storage Manager file
     * @throws IOException if an I/O error occurs
     */
    @Override
    public SeekableByteChannel newByteChannel(final Path path,
                                              final Set<? extends OpenOption> options,
                                              final FileAttribute<?>... attrs) throws IOException {
        return fileService.newByteChannel(path.toUri(), options, attrs);
    }

    /**
     * Opens a directory, returning a DirectoryStream to iterate over the entries in the directory. This method
     * works in exactly the manner specified by the Files#newDirectoryStream(java.nio.file.Path, java.nio.file.DirectoryStream.Filter)
     * method.
     *
     * @param dir the path to the directory
     * @param filter the directory stream filter
     * @return iterator to stream through the directory entries
     * @throws IOException if an I/O error occurs
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return new Neo4jfsDirectoryStream(dir);
    }

    /**
     * Create directory specified by path.
     *
     * @param dir the directory to create
     * @param attrs an optional list of file attributes to set atomically when creating the directory
     * @throws IOException unable to create directory
     */
    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        directoryService.mkdir(dir.toUri());
    }

    /**
     * Deletes the entry - directory or file - specified by path.
     *
     * @param path the path to the file to delete
     * @throws IOException thrown when delete fails, such as trying to delete non-empty directory.
     */
    @Override
    public void delete(final Path path) throws IOException {
        directoryService.delete(path.toUri());
    }

    /**
     * Copies a file or directory to a new location.
     *
     * @param source the Neo4Jfs path of the file to copy
     * @param target the Neo4Jfs path of the target file
     * @param options options specifying how the copy should be done
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void copy(final Path source,
                     final Path target,
                     final CopyOption... options) throws IOException {
        directoryService.copy(source.toUri(), target.toUri(), options);
    }

    /**
     * Moves a file or directory to a new location.
     *
     * @param source the path to the file to move
     * @param target the path to the target file
     * @param options options specifying how the move should be done
     * @throws IOException problems moving file or directory.
     */
    @Override
    public void move(final Path source,
                     final Path target,
                     final CopyOption... options) throws IOException {
        directoryService.move(source.toUri(), target.toUri(), options);
    }

    /**
     * Neo4Jfs does not support symbolic links, therefore equal paths means the same file.  In the future,
     * might need to check security, expand a symbolic link, etc., but today it remains easy
     *
     * @param path one path to the file
     * @param path2 the other path
     * @return {@code true) if the two paths refer to the same file, {@code false} otherwise.
     * @throws IOException n I/O error occurs/
     */
    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        return path.equals(path2);
    }

    /**
     * Tells whether or not a file is considered hidden.
     *
     * @param path the path to the file to test
     * @return true if file is considered hidden, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public boolean isHidden(final Path path) throws IOException {
        //  TODO: what is the correct view for the hidden attributes?
        return false;
//        return readAttributes(path, BasicFileAttributes.class).isHidden();
    }

    /**
     * Returns the FileStore representing the file store where a file is located.
     *
     * @param path the path to the file
     * @return FileStore for the Neo4Jfs file system identified by URI
     * @throws IOException an I/O error occurred
     */
    @Override
    public FileStore getFileStore(final Path path) throws IOException {
        return fileSystemService.getFileStore(path.toUri());
    }

    /**
     * Checks the existence, and optionally the accessibility, of a file.

     * @param path the path to the file to check
     * @param modes The access modes to check; may have zero elements
     * @throws IOException
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        // TODO: implement checks once security design is in place.
    }

    /**
     * Returns a file attribute view of a given type. This method works in exactly the manner specified by the
     * Files.getFileAttributeView(java.nio.file.Path, java.lang.Class<V>, java.nio.file.LinkOption...) method.
     *
     * @param path the path to the file
     * @param type the {@code Class} object corresponding to the file attribute view
     * @param options options indicating how symbolic links are handled
     * @return a file attribute view of the specific type, or {@code null} if the file does not support the specified view
     */
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path,
                                                                final Class<V> type,
                                                                final LinkOption... options) {
        //  Directory service does actual work of finding the file/directory and determining attribute view to return.
        try {
            return type.cast(directoryService.readAttributeView(path.toUri(), type, options));
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Reads a file's attributes as a bulk operation. This method works in exactly the manner specified by the
     * Files.readAttributes(Path,Class,LinkOption[]) method
     *
     * @param path the path to the file
     * @param type the {@code Class} of the file attributes required to read
     * @param options options indicating how symbolic links are handled
     * @return the file attributes
     * @throws IOException if an I/O error occurs
     */
    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path,
                                                            final Class<A> type,
                                                            final LinkOption... options) throws IOException {

        if (type == BasicFileAttributes.class) {
            FileAttributeView view = getFileAttributeView(path, BasicFileAttributeView.class, options);
            if (view instanceof BasicFileAttributeView v) {
                return type.cast(v.readAttributes());
            } else {
                return null;
            }
        } else if (type == PosixFileAttributes.class) {
            FileAttributeView view = getFileAttributeView(path, PosixFileAttributeView.class, options);
            if (view instanceof PosixFileAttributeView v) {
                return type.cast(v.readAttributes());
            } else {
                return null;
            }
        } else {
            throw new UnsupportedOperationException("Requested view not supported: " + type.getSimpleName());
        }
    }

    /**
     * Reads a set of file attributes as a bulk operation. This method works in exactly the manner specified by the
     * Files.readAttributes(Path,String,LinkOption[]) method.
     *
     * @param path the path to the file
     * @param attributes the attributes to read
     * @param options options indicating how symbolic links are handled
     * @return a map of the attributes returned; may be empty.  The map's keys are the attribute names, its values
     * the attribute values
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Map<String, Object> readAttributes(final Path path,
                                              final String attributes,
                                              final LinkOption... options) throws IOException {

        //  Determine view name for the requested attributes.
        String viewName = determineViewName(attributes);

        //  Break apart and validate the attributes provided as a string.
        List<String> validated = validateAttributes(viewName, attributes);

        //  Retrieve attributes for path.
        BasicFileAttributes fileAttributes = readAttributes(path, BasicFileAttributes.class, options);

        //  Build map of requested attributes and their valies.
        return buildAttributeMap(viewName, validated, fileAttributes);
    }

    /**
     * Sets a file attribute. This method works in exactly the manner specified by the Files.setAttribute(Path,String,Object,LinkOption...)
     * @param path the path to the file
     * @param attribute the attribute to set
     * @param value the attribute value
     * @param options options indicating how symbolic links are handled
     * @throws IOException
     */
    @Override
    public void setAttribute(final Path path,
                             final String attribute,
                             final Object value,
                             final LinkOption... options) throws IOException {
        //  Determine view name for the requested attributes.
        String viewName = determineViewName(attribute);

        //  Break apart and validate the attributes provided as a string.
        List<String> validated = validateAttributes(viewName, attribute);

        if (validated.size() != 1) {
            throw new UnsupportedOperationException("Single attribute must be specified: " + attribute);
        }

        directoryService.setAttribute(path.toUri(), viewName, validated.getFirst(), value);
    }

    /**
     * Checks for existence of file/directory at specified path.
     *
     * @param path the path to the file to test
     * @param options options indicating how symbolic links are handled
     * @return true if file/directory exists, false otherwise.
     */
    @Override
    public boolean exists(final Path path, final LinkOption... options) {
        return directoryService.exists(path.toUri());
    }

    /**
     * Creates an InputStream for reading specified file
     *
     * @param path the path to the file to open
     * @param options options specifying how the file is opened
     * @return input stream for reading file
     * @throws IOException exceptions for a multitude of reasons, file not found, access rights, etc.
     */
    @Override
    public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
        if (path instanceof Neo4jfsPath p) {
            return fileService.getInputStream(p.toUri());
        } else {
            return null;
        }
    }

    /**
     * Creates an OutputStream for writing specified file, creating the file if it does not exist.
     *
     * @param path the path to the file to open or create
     * @param options options specifying how the file is opened
     * @return output stream for writing file
     * @throws IOException exceptions for a multitude of reasons, file not found, access rights, etc.
     */
    public OutputStream newOutputStream(final Path path, final OpenOption... options) throws IOException {
        if (path instanceof Neo4jfsPath p) {
            return fileService.getOutputStream(p.toUri());
        } else {
            return null;
        }
    }

    /**
     * Removes file system from registry, called by file system when it's marked for closing.
     *
     * @param uri base URI of file system
     */
    void removeFileSystem(final URI uri) {
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
     *
     * @param uri Neo4Jfs URI, could be fully-qualified with path or not.
     * @return URI with just scheme and partition (host), no path is kept.
     */
    private URI truncateUri(final URI uri) {
        validateUri(uri);
        return uri.resolve(Neo4jfsConstants.NAME_ROOT_DIRECTORY);
    }

    /**
     * Identify the view name associated with the attributes
     *
     * @param attributes the complete list of attributes
     * @return the view name
     * @throws UnsupportedOperationException view name is not known or supported by Neo4Jfs
     */
    private String determineViewName(final String attributes) {
        int colonIndex = attributes.indexOf(':');
        if (colonIndex > 0) {
            String viewName = attributes.substring(0, colonIndex);
            if (SUPPORTED_ATTRIBUTE_VIEW_NAME.contains(viewName)) {
                return viewName;
            }

            //  The view name provided is unknown, unsupported, whatever.
            throw new UnsupportedOperationException("Invalid view name: " + viewName);
        }

        return DEFAULT_ATTRIBUTE_VIEW_NAME;
    }

    /**
     * Build the attribute map for the requested view
     *
     * @param viewName requested view
     * @param attributes list of attributes to return
     * @param file the file/directory for which we want attributes
     * @return Map of requested attributes and their values
     */
    private Map<String,Object> buildAttributeMap(final String viewName,
                                                 final List<String> attributes,
                                                 final BasicFileAttributes file) {
        switch (viewName) {
            case ATTRIBUTE_VIEW_NAME_BASIC:
                return buildAttributeMapBasic(attributes, file);
            default:
                //  Should never get here ....
                throw new UnsupportedOperationException(viewName);
        }
    }

    /**
     * Build the attribute map for the "basic" view.
     *
     * @param attributes validated list of attributes to return
     * @param file the file/directory for which we want attributes
     * @return Map of requested attributes and their values.
     */
    private Map<String,Object> buildAttributeMapBasic(final List<String> attributes,
                                                      final BasicFileAttributes file) {
        Map<String,Object> toReturn = new HashMap<>(attributes.size());
        for (String attr : attributes) {
            switch (attr) {
                case BASIC_ATTRIBUTE_CREATE_TIME:
                    toReturn.put(BASIC_ATTRIBUTE_CREATE_TIME, file.creationTime());
                    break;
                case BASIC_ATTRIBUTE_FILE_KEY:
                    toReturn.put(BASIC_ATTRIBUTE_FILE_KEY, file.fileKey());
                    break;
                case BASIC_ATTRIBUTE_LAST_ACCESS_TIME:
                    toReturn.put(BASIC_ATTRIBUTE_LAST_ACCESS_TIME, file.lastAccessTime());
                    break;
                case BASIC_ATTRIBUTE_LAST_MODIFIED_TIME:
                    toReturn.put(BASIC_ATTRIBUTE_LAST_MODIFIED_TIME, file.lastModifiedTime());
                    break;
                case BASIC_ATTRIBUTE_IS_DIRECTORY:
                    toReturn.put(BASIC_ATTRIBUTE_IS_DIRECTORY, file.isDirectory());
                    break;
                case BASIC_ATTRIBUTE_IS_OTHER:
                    toReturn.put(BASIC_ATTRIBUTE_IS_OTHER, file.isOther());
                    break;
                case BASIC_ATTRIBUTE_IS_REGULAR_FILE:
                    toReturn.put(BASIC_ATTRIBUTE_IS_REGULAR_FILE, file.isRegularFile());
                    break;
                case BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK:
                    toReturn.put(BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK, file.isSymbolicLink());
                    break;
                case BASIC_ATTRIBUTE_SIZE:
                    toReturn.put(BASIC_ATTRIBUTE_SIZE, file.size());
                    break;
                default:
                    // By this point we should be good, so just ignore if something unexpected seen.
            }
        }

        return toReturn;
    }

    /**
     * Validate the attributes for the requested view.
     * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html#readAttributes(java.nio.file.Path,java.lang.String,java.nio.file.LinkOption...)"/>
     *
     * @param viewName view name
     * @param attributes attributes to validate separated by common
     * @return subset of attributes for this view, or all if '*' provided.
     */
    private List<String> validateAttributes(final String viewName,
                                            final String attributes) {
        if (attributes == null || attributes.isEmpty()) {
            throw new IllegalArgumentException("No attributes specified.");
        }

        //  String view name from beginning of attributes string
        String noViewAttributes = attributes;
        if (attributes.startsWith(viewName + ATTRIBUTE_VIEW_SEPARATOR)) {
            noViewAttributes = attributes.substring(attributes.indexOf(':') + 1);
        }

        //  Break apart attributes into list we can work with.
        List<String> split = Arrays.asList(noViewAttributes.split(ATTRIBUTE_SEPARATOR));

        switch (viewName) {
            case ATTRIBUTE_VIEW_NAME_BASIC:
                return validateAttribsBasic(split);
            default:
                //  view name _should_ have been validated earlier, therefore don't expect to get here.
                throw new UnsupportedOperationException(viewName);
        }
    }

    /**
     * Validate the attribute names for the "basic" view.
     *
     * @param attributes list of attributes to validate
     * @return subset of attributes for this view, or all if '*' provided.
     */
    private List<String> validateAttribsBasic(final List<String> attributes) {
        if (attributes.size() == 1 && attributes.getFirst().equals(ATTRIBUTE_WILDCARD_ALL)) {
            return BASIC_ATTRIBUTES_ALL;
        } else {
            List<String> unknown = attributes.stream().filter(a -> !BASIC_ATTRIBUTES_ALL.contains(a)).toList();
            if (unknown.isEmpty()) {
                return attributes;
            } else {
                throw new IllegalArgumentException("Invalid attribute: " + unknown.stream().collect(Collectors.joining(",")));
            }
        }
    }

    /**
     * Validate all provided URIs to ensure completeness for Neo4Jfs
     * @param uri URI to validate.
     */
    private void validateUri(final URI uri) {

        //  Each implemented file system has a unique scheme.
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals(getScheme())) {
            throw new IllegalArgumentException("URI scheme must be " + getScheme() + ".");
        }

        //  URI's host provides Neo4Jfs partition name.
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("Neo4Jfs partition must be specified as URI host.");
        }
    }
}
