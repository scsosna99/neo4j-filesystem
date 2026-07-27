/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.model.storage.StorageFileInfo;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileBuilder;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.database.repository.FileEntryRepository;
import dev.scottsosna.neo4jfs.exception.Neo4jfsUnknownEntryException;
import dev.scottsosna.neo4jfs.security.AccessManager;
import dev.scottsosna.neo4jfs.service.util.CallbackOutputStream;
import dev.scottsosna.neo4jfs.service.util.CallbackSeekableByteChannel;
import dev.scottsosna.neo4jfs.storage.StorageManager;
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

/**
 * Service manages Neo4Jfs files.
 */
@Service
public class FileServiceImpl extends BaseNeo4jfsService implements FileService {

    /**
     * Repository for managing {@code FileEntry} nodes within Neo4J database.
     */
    private final FileEntryRepository repository;

    /**
     * Directory services manages directories/subdirectories and, important here, associated files.
     */
    private final DirectoryService directoryService;

    /**
     * Storage Manaager instance for persisting physical files into Neo4Jfs.
     */
    private final StorageManager storageManager;

    /**
     * Logger for this class.
     */
    private final static Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    /**
     * Pre-created open options when SeekableByteChannel is opened for read-only access.
     */
    private final static Set<StandardOpenOption> READ_ONLY_OPTIONS = Set.of(StandardOpenOption.READ);

    /**
     * Constructor
     * @param repository database component for files
     * @param directoryService service for directories containing files
     * @param storageManager physical storage of the files managed by Neo4Jfs
     * @param accessManager checks access permissions for service
     */
    public FileServiceImpl(final FileEntryRepository repository,
                           final DirectoryService directoryService,
                           final StorageManager storageManager,
                           final AccessManager accessManager) {
        super(accessManager);
        this.repository = repository;
        this.directoryService = directoryService;
        this.storageManager = storageManager;
    }

    /**
     * Create new, empty file for given URI
     * @param uri fully-qualified Neo4Jfs file URI
     * @throws IOException for whatever reason, the file could not be created
     */
    public void create (final URI uri) throws IOException{
        createWork(uri, null);
    }

    /**
     * Copy the input stream into a new file
     * @param uri fully-qualified Neo4Jfs file URI
     * @param is input stream for reading file contents
     * @throws IOException if I/O fails during create for any reason.
     */
    public void create (final URI uri, final InputStream is) throws IOException {
        createWork(uri, is);
    }

    /**
     * Helper method for creating/persisting local file into Neo4Jfs
     * @param uri Neo4Jfs file URI
     * @param sourceFile local file to persist
     * @throws IOException file cannot be created
     */
    public void create (final URI uri, final Path sourceFile) throws IOException{
        create(uri, Files.newInputStream(sourceFile));
    }

    /**
     * Copies source file to target directory or file
     * @param sourceUri Fully-qualified URI for source file
     * @param targetUri Fully-qualified URI for target file or directory.
     * @param options {@code CopyOption}s for this copy
     * @throws IOException any I/O error during copy
     */
    public void copy(URI sourceUri, URI targetUri, final CopyOption... options) throws IOException {
        sourceUri = checkUri(sourceUri);
        targetUri = checkUri(targetUri);

        //  Verify source file exists.
        FileEntry sourceFile = prologueExistingFile(sourceUri, false);

        //  First check complete URI for existence.
        List<BaseEntry> targetEntries = directoryService.find(targetUri);
        DirectoryEntry targetDirectory = null;
        if (!targetEntries.isEmpty()) {
            //  Check whether URI was directory or file.
            switch (targetEntries.getLast()) {
                case FileEntry fe:
                    targetDirectory = (DirectoryEntry) targetEntries.get(targetEntries.size() - 2);
                    break;
                case DirectoryEntry de:
                    targetDirectory = de;
                    break;
                default:
                    throw new Neo4jfsUnknownEntryException(targetEntries.getLast().getClass().getSimpleName());
            }
        } else {
            //  Target URI not found, let's see if parent exists AND is a directory.
            targetEntries = directoryService.find(Path.of(targetUri).getParent().toUri());
            if (targetEntries.isEmpty()) {
                //  Invalid target for copy.
                throw new NoSuchFileException(targetUri.toString());
            }

            //  Parent MUST be a directory.
            BaseEntry last = targetEntries.getLast();
            if (!(last instanceof DirectoryEntry)) {
                throw new NotDirectoryException(targetUri.toString());
            }
            targetDirectory = (DirectoryEntry) last;
        }

        //  Do the actual work, including checking access.
        copy (sourceFile, targetUri, targetDirectory, options);
    }

    /**
     * Copies source file to target directory or file
     * @param sourceFile entry of source file as Neo4J node
     * @param targetUri fully-qualified URI for target file or directory.
     * @param targetDirectory destination directory for copied file as Neo4J node
     * @param options {@code CopyOption}s for this copy
     * @throws IOException any I/O error during copy
     */
    public void copy(final FileEntry sourceFile,
                     final URI targetUri,
                     final DirectoryEntry targetDirectory,
                     final CopyOption... options) throws IOException {

        //  Must have both read/write for target directory.
        checkAccess(targetDirectory, AccessMode.READ, AccessMode.WRITE);

        //  Is there already a file or subdir with the same name?
        String fileName = Path.of(targetUri).getFileName().toString();
        verifyNameUniqueness(targetUri, targetDirectory, fileName, checkForCopyOption(StandardCopyOption.REPLACE_EXISTING, options));

        //  Copy physical file
        StorageFileInfo info = storageManager.copyFile(targetUri, sourceFile.getStorageId());

        //  When the target specified in URI is same as the name of the target directory into which the file us
        //  being copied, the new file name is the same as the source file name.
        if (fileName.equals(targetDirectory.getName())) {
            fileName = sourceFile.getName();
        }

        //  Create/persist new file entry.
        FileEntry f = new FileBuilder(targetDirectory)
            .setName(fileName)
            .setUserName(accessManager.userName())
            .setStorageId(info.getStorageId())
            .setSize(info.getSize())
            .build();
        repository.create(targetUri, f);

        //  Add to the parent directory.
        directoryService.addFile(targetUri, targetDirectory, f);
    }

    /**
     * Delete a file by URI
     * @param uri fully-qualified URI for Neo4Jfs file
     * @throws IOException if I/O fails during delete.
     */
    public void delete(final URI uri) throws IOException {
        FileEntry fe = prologueExistingFile(uri, false);
        checkAccess(fe, AccessMode.READ, AccessMode.WRITE);
        deleteWork(uri, fe);    //  checks WRITE access
    }

    /**
     * Delete a file by Neo4J node ID
     * @param fsUri Neo4Jfs files system URI
     * @param nodeId node ID of FileEntry to delete
     * @throws IOException thrown when delete fails, most like StorageManager but could be for other reasons
     */
    @Override
    public void delete(final URI fsUri, final String nodeId) throws IOException {
        checkUri(fsUri);
        FileEntry file = repository.load(fsUri, nodeId);
        deleteWork(fsUri, file);
    }

    /**
     * Create input stream for reading persisted file.
     * @param uri fully-qualified URI for file to read
     * @return InputStream for reading file contents
     * @throws IOException if I/O fails during read.
     */
    public InputStream getInputStream(final URI uri) throws IOException {
        checkUri(uri);
        FileEntry fe = prologueExistingFile(uri, false);
        checkAccess(fe, AccessMode.READ);
        repository.updateLastAccessed(uri, fe, FileEntry.class);
        return storageManager.getFileInputStream(uri, fe.getStorageId());
    }

    /**
     * Create output stream for writing to file persisted in Storage Manager
     * @param uri fully-qualified URI for file to write to
     * @return OutputStream for writing file contents
     * @throws IOException if I/O fails during write.
     */
    public OutputStream getOutputStream(final URI uri) throws IOException {
        FileEntry fe = prologueExistingFile(uri, true);
        checkAccess(fe, AccessMode.WRITE);
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
    public SeekableByteChannel newByteChannel(final URI uri, Set<? extends OpenOption> options,
                                              final FileAttribute<?>... attrs) throws IOException {

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
    private FileEntry createWork(final URI uri, final InputStream is) throws IOException {
        checkUri(uri);

        //  Does parent directory exist and is it a directory?
        BaseEntry parent = directoryService.parent(uri);
        if (!(parent instanceof DirectoryEntry)) {
            throw new NoSuchFileException("%s: no such file or directory".formatted(uri.resolve(".")));
        }

        //  Confirm user has permissions to create file in parent directory.
        DirectoryEntry parentDirectory = (DirectoryEntry) parent;
        checkAccess(parentDirectory, AccessMode.WRITE, AccessMode.EXECUTE);

        //  Is there already a file or subdir with the same name?
        String fileName = Path.of(uri).getFileName().toString();
        verifyNameUniqueness(uri, parentDirectory, fileName, false);

        //  When InputStream provided, we have data to persist; without create an empty file
        StorageFileInfo info = (is != null) ? storageManager.createFile(uri, is) : storageManager.createFile(uri);

        //  Create/persist new file entry.
        FileEntry f = new FileBuilder(parentDirectory)
            .setName(fileName)
            .setUserName(accessManager.userName())
            .setStorageId(info.getStorageId())
            .setSize(info.getSize())
            .build();
        repository.create(uri, f);

        //  Add to the parent directory.
        directoryService.addFile(uri, parentDirectory, f);

        // Newly-built entry hasn't had its permissions determined as when existing entry is retrieved from Neo4J so
        // manually force permissions to be determined (always from parent directory)
        f.deriveInheritedPermissions(parentDirectory);

        return f;
    }

    /**
     * Does actual delete of a file in the Neo4J file system.
     * @param uri specifies Neo4Jfs filesystem
     * @param file specific file to delete
     * @throws IOException for whatever reason, the file could not be deleted
     */
    private void deleteWork(final URI uri, final FileEntry file) throws IOException {

        //  Confirm user has permissions to delete the file.
        checkAccess(file, AccessMode.WRITE);

        //  Delete the node first and then the file.
        if (!repository.delete(uri, file.getId())) {
            throw new IOException("Unable to delete file entry %s.".formatted(file.getName()));
        }

        //  Check for any other files which may share same external storage, in which case we won't delete.
        List<FileEntry> files = repository.findByStorageId(uri, file.getStorageId());
        if (files == null || files.isEmpty()) {
            try {
                //  Not ideal if physical file can't be deleted from storage manager but, as far as file system knows,
                //  the file is gone and inaccessible once the node is deleted.  Probably need a util to clean up orphans
                storageManager.deleteFile(uri, file.getStorageId());
            } catch (IOException e) {
                logger.info("{}: unable to delete file {} from storage manager: {}", file.getName(), file.getStorageId(), e.getMessage());
            }
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
    private FileEntry prologueExistingFile(final URI uri, final boolean createIfNotFound) throws IOException {
        checkUri(uri);

        List<BaseEntry> parts = directoryService.find(uri);
        BaseEntry entry = null;
        if (parts.isEmpty()) {
            if (createIfNotFound) {
                //  NOTE: createWork checks for write access on parent directory.
                entry = createWork(uri, null);
            } else {
                throw new NoSuchFileException("%s: no such file or directory".formatted(uri));
            }
        } else {
            //  Parent directory requires execute permissions.
            checkAccess(parts.get(parts.size() - 2), AccessMode.EXECUTE);

            //  Requires at least READ access, caller checks for WRITE if necessary.
            entry = parts.getLast();
            checkAccess(entry, AccessMode.READ);
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
    private void registerDeleteCallback(final CallbackSeekableByteChannel channel, final URI uri) {
        channel.registerCallback(() -> {
            try {
                delete(uri);
            } catch (IOException e) {
                logger.error("Unable to delete file {} on close: {}", uri, e.getMessage());
            }
        });
    }

    /**
     * Create a read-only seekable byte channel for a Neo4Jfs file.
     * @param uri URI to Neo4Jfs file
     * @param flags set of options for opening physical file
     * @param attrs file attributes
     * @return seekable byte channel
     * @throws IOException if I/O fails while creating channel
     */
    private SeekableByteChannel newByteChannelReadOnly(final URI uri,
                                                       final OpenOptionFlags flags,
                                                       final FileAttribute<?>... attrs) throws IOException {

        // File must already exist for read-only access.
        FileEntry f = prologueExistingFile(uri, false);
        checkAccess(f, AccessMode.READ);

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

    /**
     * Create a read-write or write-only seekable byte channel for a Neo4Jfs file, based on flags.
     * @param uri URI to Neo4Jfs file
     * @param flags set of options for opening physical file
     * @param attrs file attributes
     * @return seekable byte channel
     * @throws IOException if I/O fails while creating channel
     */
    private SeekableByteChannel newByteChannelWrite(final URI uri,
                                                    final OpenOptionFlags flags,
                                                    final FileAttribute<?>... attrs) throws IOException {

        FileEntry f = null;
        if (flags.createNew) {
            //  {@StandardOpenOption#CREATE_NEW} means that file cannot already exist.
            try {
                //  Attempt to retrieve file and throw exception if it already exists.
                f = prologueExistingFile(uri, false);
                throw new FileAlreadyExistsException(uri.toString());
            } catch (IOException ioe) {
                //  Success, file doesn't already exist so create new, empty one.
                //  NOTE: createWork checks for write access on parent directory.
                f = createWork(uri, null);
            }
        } else {
            //  File may exist or could be created, based on the flag.
            f = prologueExistingFile(uri, flags.create);
            checkAccess(f, AccessMode.WRITE);
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
    private void updateSize(final URI uri) {

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

    /**
     * Verifies that name of new file doesn't conflict with existing file/directory in the destination directory.
     * @param uri URI for new file
     * @param directory target directory where new file will be created
     * @param fileName proposed name of new file which should not exist in current directory.
     * @throws IOException when new file name would conflict with existing, other IO errors.
     */
    private void verifyNameUniqueness(final URI uri,
                                      final DirectoryEntry directory,
                                      final String fileName,
                                      final boolean replace) throws IOException {
        //  Must have READ access to parent directory to check name uniqueness
        checkAccess(directory, AccessMode.READ);

        //  Is there already a file or subdir with the same name?
        BaseEntry child = repository.findNamedChild(uri, directory.getId(), fileName);
        if (child != null) {
            if (replace) {
                switch (child) {
                    case FileEntry fe:
                        deleteWork(uri, fe);
                        break;
                    case DirectoryEntry de:
                        directoryService.delete(uri);
                        break;
                    default:
                        throw new Neo4jfsUnknownEntryException(child.getClass().getSimpleName());
                }
            } else {
                throw new FileAlreadyExistsException(uri.toString());
            }
        }
    }

    @PostConstruct
    private void init() {
        directoryService.registerFileService(this);
    }

    /**
     * Helper class for managing/checking {@code OpenOption} provided.
     */
    private class OpenOptionFlags {

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
         * @return set of OpenOption flags
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
