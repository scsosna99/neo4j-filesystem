package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.model.storage.StorageFileInfo;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.database.repository.FileEntryRepository;
import dev.scottsosna.neo4jfs.storage.StorageManager;
import dev.scottsosna.neo4jfs.service.util.CallbackOutputStream;
import dev.scottsosna.neo4jfs.service.util.CallbackSeekableByteChannel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FileServiceImpl extends BaseNeo4jfsService implements FileService {

    private final FileEntryRepository repository;
    private final DirectoryService directoryService;
    private final StorageManager storageManager;

    private final static Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    private final static Set<StandardOpenOption> READ_ONLY_OPTIONS = Set.of(StandardOpenOption.READ);

    public FileServiceImpl(FileEntryRepository repository,
                           DirectoryService directoryService,
                           StorageManager storageManager) {
        this.repository = repository;
        this.directoryService = directoryService;
        this.storageManager = storageManager;
    }

    /**
     * Create new, empty file for given URI
     * @param uri Neo4Jfs file URI
     * @throws IOException for whatever reason, the file could not be created
     */
    public void create (URI uri) throws IOException{
        createWork(uri, null);
    }

    /**
     * Copy the input stream into a new file
     * @param uri Neo4Jfs file URI
     * @param is input stream for file contents
     * @throws IOException for whatever reason, the file could not be created
     */
    public void create (URI uri, InputStream is) throws IOException {
        createWork(uri, is);
    }

    /**
     * Helper method for creating/persisting local file into Neo4Jfs
     * @param uri Neo4Jfs file URI
     * @param sourceFile local file to persist
     * @throws IOException file cannot be created
     */
    public void create (URI uri, Path sourceFile) throws IOException{
        create(uri, Files.newInputStream(sourceFile));
    }

    public void delete(URI uri) throws IOException {
        FileEntry fe = prologueExistingFile(uri, false);
        deleteWork(uri, fe);
    }

    /**
     * Delete a file by Neo4J node ID
     * @param uri URI at minimum specifies partition
     * @param nodeId node ID of FileEntry to delete
     * @throws IOException thrown when delete fails, most like StorageManager but could be for other reasons
     */
    @Override
    public void delete(URI uri, String nodeId) throws IOException {
        checkUri(uri);
        FileEntry file = repository.load(uri, nodeId);
        deleteWork(uri, file);
    }

    public InputStream getInputStream(URI uri) throws IOException {
        checkUri(uri);
        FileEntry fe = prologueExistingFile(uri, false);
        repository.updateLastAccessed(uri, fe, FileEntry.class);
        return storageManager.getFileInputStream(uri, fe.getStorageId());
    }

    public OutputStream getOutputStream(URI uri) throws IOException {
        FileEntry fe = prologueExistingFile(uri, true);
        OutputStream os = storageManager.getFileOutputStream(uri, fe.getStorageId());

        //  Register callback to update file size once stream is closed.
        if (os instanceof CallbackOutputStream sos) {
            sos.registerCallback(() -> updateSize(uri));
        }

        return os;
    }

    /**
     * Create new seekable byte channel for NeofJfs file
     * @param uri fully-qualified Neo4Jfs URI for file
     * @param options set of open options
     * @param attrs attributes for file
     * @return seekable byte channel
     * @throws IOException if I/O problem occurred
     */
    public SeekableByteChannel newByteChannel(URI uri, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {

        //  Load, verify, normalize flags.
        OpenOptionFlags flags = new OpenOptionFlags(options);

        if (flags.write) {
            return newByteChannelWrite(uri, flags, attrs);
        } else {
            return newByteChannelReadOnly(uri, flags, attrs);
        }
    }



    /**
     * Does the actual work of creating a file.  Other than how the file is created via Storage Manager, the
     * steps are identical, hence the helper method.
     * @param uri Neo4Jfs file URI
     * @param is when provided, InputStream for file contents
     * @return FileEntry for just-created file
     * @throws IOException for whatever reason, the file could not be created
     */
    private FileEntry createWork(URI uri, InputStream is) throws IOException {
        checkUri(uri);

        //  Does parent directory exist and is it a directory?
        BaseEntry parent = directoryService.parent(uri);
        if (!(parent instanceof DirectoryEntry)) {
            throw new NoSuchFileException("%s: no such file or directory".formatted(uri.resolve(".")));
        }

        //  Is there already a file or subdir with the same name?
        BaseEntry child = repository.findNamedChild(uri, parent.getId(), Path.of(uri).getFileName().toString());
        if (child != null) {
            throw new FileAlreadyExistsException(uri.toString());
        }

        //  When InputStream provided, we have data to persist; without create an empty file
        StorageFileInfo info = (is != null) ? storageManager.createFile(uri, is) : storageManager.createFile(uri);

        //  Create/persist new file entry.
        FileEntry f = repository.create(uri, Path.of(uri).getFileName().toString(), info.getStorageId(), info.getSize());

        //  Add to the parent directory.
        directoryService.addFile(uri, (DirectoryEntry) parent, f);

        return f;
    }

    /**
     * Does actual delete of a file in the Neo4J file system.
     * @param uri specifies Neo4Jfs filesystem
     * @param file specific file to delete
     * @throws IOException for whatever reason, the file could not be deleted
     */
    private void deleteWork(URI uri, FileEntry file) throws IOException {

        //  Delete the node first and then the file.
        if (!repository.delete(uri, file.getId())) {
            throw new IOException("Unable to delete file entry %s.".formatted(file.getName()));
        }

        try {
            //  Not ideal if physical file can't be deleted from storage manager but, as far as file system knows,
            //  the file is gone and inaccessible once the node is deleted.  Probably need a util to clean up orphans
            storageManager.deleteFile(uri, file.getStorageId());
        } catch (IOException e) {
            logger.info("{}: unable to delete file {} from storage manager: {}", file.getName(), file.getStorageId(), e.getMessage());
        }
    }

    /**
     * Same preliminary checks for action on specific file: attempt to get all entries in the path specified
     * by URI and ensure the last entry on path is file.
     * @param uri URI for (hopefully) specific file
     * @param createIfNotFound if true, create a new file if not found
     * @return FileEntry for the file specified
     * @throws IOException path not found, path isn't a file, etc.
     */
    private FileEntry prologueExistingFile(URI uri, boolean createIfNotFound) throws IOException {
        checkUri(uri);

        List<BaseEntry> parts = directoryService.find(uri);
        BaseEntry entry = null;
        if (parts.isEmpty()) {
            if (createIfNotFound) {
                entry = createWork(uri, null);
            } else {
                throw new NoSuchFileException("%s: no such file or directory".formatted(uri));
            }
        } else {
            entry = parts.getLast();
        }

        if (entry instanceof FileEntry f) {
            return f;
        } else {
            throw new NoSuchFileException(uri.toString());
        }
    }

    /**
     * Register a deleteFile() call that is executed upon closing the byte channel.
     * @param channel the channel on which to register the callback.
     * @param uri Neo4Jfs URI for the file to delete.
     */
    private void registerDeleteCallback(CallbackSeekableByteChannel channel, URI uri) {
        channel.registerCallback(() -> {
            try {
                delete(uri);
            } catch (IOException e) {
                logger.error("Unable to delete file {} on close: {}", uri, e.getMessage());
            }
        });
    }

    private SeekableByteChannel newByteChannelReadOnly(URI uri,
                                                       OpenOptionFlags flags,
                                                       FileAttribute<?>... attrs) throws IOException {

        // File must already exist for read-only access.
        FileEntry f = prologueExistingFile(uri, false);

        //  Storage Manager creates the channel
        SeekableByteChannel channel = storageManager.getSeekableByteChannel(uri, f.getStorageId(), READ_ONLY_OPTIONS);

        //  Read-only file can be deleted on close, in which case we need to delete file entry as well.
        if (flags.deleteOnClose) {
            channel = new CallbackSeekableByteChannel(storageManager.getSeekableByteChannel(uri, f.getStorageId(), READ_ONLY_OPTIONS));
            registerDeleteCallback((CallbackSeekableByteChannel) channel, uri);
            return channel;
        }

        return channel;
    }

    private SeekableByteChannel newByteChannelWrite(URI uri,
                                                    OpenOptionFlags flags,
                                                    FileAttribute<?>... attrs) throws IOException {

        FileEntry f = null;
        if (flags.createNew) {
            //  {@StandardOpenOption#CREATE_NEW} means that file cannot already exist.
            try {
                //  Attempt to retrieve file and throw exception if it already exists.
                f = prologueExistingFile(uri, false);
                throw new FileAlreadyExistsException(uri.toString());
            } catch (IOException ioe) {
                //  Success, file doesn't already exist so create new, empty one.
                f = createWork(uri, null);
            }
        } else {
            //  File may exist or could be created, based on the flag.
            f = prologueExistingFile(uri, flags.create);
        }

        //  Callback on close always required for writeable files, either to delete the file on close or to update size.
        CallbackSeekableByteChannel channel = new CallbackSeekableByteChannel(storageManager.getSeekableByteChannel(uri, f.getStorageId(), flags.toSet()));
        if (flags.deleteOnClose) {
            registerDeleteCallback(channel, uri);
        } else {
            channel.registerCallback(() -> updateSize(uri));
        }

        return channel;
    }

    /**
     * Retrieve file size from storage manager and updates FileEntry.
     * @param uri URI for Neo4Jfs file
     */
    private void updateSize(URI uri) {

        try {
            //  Retrieve complete path based on URI.
            List<BaseEntry> pathEntries = directoryService.find(uri);
            if (pathEntries == null || pathEntries.isEmpty()) {
                logger.warn("Unable to update file size for {}: path not found.", uri);
                return;
            }

            //  Path entries found, last entry expected to be file.
            if (pathEntries.getLast() instanceof FileEntry fe) {
                //  Retrieve file size from storage manager and update FileEntry.
                StorageFileInfo info = storageManager.getFileInfo(uri, fe.getStorageId());
                fe.setSize(info.getSize());
                repository.save(uri, fe, FileEntry.class);
            } else {
                logger.warn("Unable to update file size for {}: path not a file.", uri);
            }
        } catch (IOException e) {
            //  Nuisance but not fatal, log the exception and move on.
            logger.warn("Unable to update file size for {}: {}", uri, e.getMessage());
        }
    }

    @PostConstruct
    private void init() {
        directoryService.registerFileService(this);
    }

    /**
     * Helper class for managing/checking {@code OpenOption} provided.
     */
    protected class OpenOptionFlags {

        //  Flag per each {@code StandardOpenOption} value
        boolean append = false;
        boolean create = false;
        boolean createNew = false;
        boolean deleteOnClose = false;
        boolean dsync = false;
        boolean read = false;
        boolean sparse = false;
        boolean sync = false;
        boolean truncateExisting = false;
        boolean write = false;

        /**
         * Constructor take options and assign flags accordingly.
         * @param options set of open options
         */
        OpenOptionFlags(Set<? extends OpenOption> options) {

            //  Iterate over set expecting only StandardOpenOption values.
            options.forEach(o -> {
                if (o instanceof StandardOpenOption so) {
                    switch (so) {
                        case APPEND:
                            append = true;
                            break;
                        case CREATE:
                            create = true;
                            break;
                        case CREATE_NEW:
                            createNew = true;
                            break;
                        case DELETE_ON_CLOSE:
                            deleteOnClose = true;
                            break;
                        case DSYNC:
                            dsync = true;
                            break;
                        case READ:
                            read = true;
                            break;
                        case SPARSE:
                            sparse = true;
                            break;
                        case SYNC:
                            sync = true;
                            break;
                        case TRUNCATE_EXISTING:
                            truncateExisting = true;
                            break;
                        case WRITE:
                            write = true;
                            break;
                    }
                } else {
                    //  All {@code OpenOptions} are invalid.
                    throw new IllegalArgumentException("Unsupported open option: %s".formatted(o));
                }
            });

            //  Make sure flags are valid.
            validateNormalizeFlags();
        }

        /**
         * Return set of OpenOption based on flags, skipping those not applicable to Neo4Jfs:
         * -CREATE and CREATE_NEW: Neo4Jfs file prerequisite for creating channel, therefore file exists in storage manager
         * -SPARSE only applies when file created (assuming storage is local disk) and doesn't apply here as well
         * @return
         */
        Set<OpenOption> toSet() {
            Set<OpenOption> set = new HashSet<>();
            if (append) set.add(StandardOpenOption.APPEND);
            if (dsync) set.add(StandardOpenOption.DSYNC);
            if (read) set.add(StandardOpenOption.READ);
            if (sync) set.add(StandardOpenOption.SYNC);
            if (truncateExisting) set.add(StandardOpenOption.TRUNCATE_EXISTING);
            if (write) set.add(StandardOpenOption.WRITE);
            return set;
        }

        /**
         * Check OpenOption flags for correctness, consistency.
         * Based on <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html#newByteChannel(java.nio.file.Path,java.util.Set,java.nio.file.attribute.FileAttribute...)"</a>
         */
        private void validateNormalizeFlags() {
            //  READ and APPEND not allowed together.
            if (read && append) {
                throw new IllegalArgumentException("READ + APPEND not allowed.");
            }

            //  APPEND and TRUNCATE_EXISTING not allowed togeether.
            if (append && truncateExisting) {
                throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed.");
            }

            // When neither read or write explicitly stated, default to read.
            if (!read && !write && !append) {
                read = true;
            }

            //  CREATE ignored when CREATE_NEW is specified.
            if (create && createNew) {
                create = false;
            }
        }
    }
}
