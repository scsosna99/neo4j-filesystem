package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.model.storage.StorageFileInfo;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.database.repository.FileEntryRepository;
import dev.scottsosna.neo4jfs.storage.StorageManager;
import dev.scottsosna.neo4jfs.storage.util.CallbackOutputStream;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

@Service
public class FileServiceImpl extends BaseNeo4jfsService implements FileService {

    private final FileEntryRepository repository;
    private final DirectoryService directoryService;
    private final StorageManager storageManager;

    private final static Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

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
        FileEntry fe = prologueExistingFile(uri, false);
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
            throw new RuntimeException("%s: File already exists".formatted(uri));
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
            throw new RuntimeException("Unable to delete file entry.");
        }

        //  Not ideal if physic file can't be deleted from storage manager but, as far as file system knows,
        //  the file is gone and inaccessible once the node is deleted.  Probably need a util to clean up orphans
        storageManager.deleteFile(uri, file.getStorageId());
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
            throw new RuntimeException("%s: Not a file".formatted(uri));
        }
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
}
