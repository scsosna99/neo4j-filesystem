package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.model.storage.StorageFileInfo;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.database.repository.FileEntryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class FileServiceImpl extends BaseNeo4jfsService implements FileService {

    private final FileEntryRepository repository;
    private final DirectoryService directoryService;
    private final StorageManager storageManager;

    public FileServiceImpl(FileEntryRepository repository,
                           DirectoryService directoryService,
                           StorageManager storageManager) {
        this.repository = repository;
        this.directoryService = directoryService;
        this.storageManager = storageManager;
    }

    public void create (URI uri, InputStream inputStream) throws IOException {
        checkUri(uri);

        //  Does parent directory exist and is it a directory?
        BaseEntry parent = directoryService.parent(uri);
        if (!(parent instanceof DirectoryEntry)) {
            throw new RuntimeException("%s: Not a directory");
        }

        //  Is there already a file or subdir with the same name?
        BaseEntry child = repository.findNamedChild(uri, parent.getId(), Path.of(uri).getFileName().toString());
        if (child != null) {
            throw new RuntimeException("%s: File already exists".formatted(uri));
        }

        //  Checks pass, first persist the file in storage manager.
        StorageFileInfo info = storageManager.storeFile(uri, inputStream);

        //  Create/persist new file entry.
        FileEntry f = repository.create(uri, Path.of(uri).getFileName().toString(), info.getStorageId(), info.getSize());

        //  Add to the parent directory.
        directoryService.addFile(uri, (DirectoryEntry) parent, f);
    }

    public void create (URI uri, File sourceFile) throws IOException{
        checkUri(uri);

        try {
            create(uri, Files.newInputStream(Path.of(sourceFile.getAbsolutePath())));
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to read source file.", ioe);
        }
    }

    public void delete(URI uri) throws IOException {
        checkUri(uri);

        List<BaseEntry> parts = directoryService.find(uri);
        if (parts.isEmpty()) {
            throw new RuntimeException("%s: no such file or directory".formatted(uri));
        }

        if (parts.getLast() instanceof FileEntry f) {
            deleteWork(uri, f);
        } else {
            throw new RuntimeException("%s: Not a file".formatted(uri));
        }
    }

    @Override
    public void delete(URI uri, String nodeId) throws IOException {
        checkUri(uri);
        FileEntry file = repository.load(uri, nodeId);
        deleteWork(uri, file);
    }

    private void deleteWork(URI uri, FileEntry file) throws IOException {

        //  Delete the node first and then the file.
        if (!repository.delete(uri, file.getId())) {
            throw new RuntimeException("Unable to delete file entry.");
        }

        //  Not ideal if file can't be deleted from storage manager but, as far as file system knows,
        //  the file is gone and inaccessible because the node has been deleted.  Probably need a util
        //  to clean up orphaned files.
        try {
            storageManager.deleteFile(file.getStorageId());
        } catch (Exception e) {
            System.out.println("Unable to delete file from storage manager." + e);
        }
    }

    @PostConstruct
    private void init() {
        directoryService.registerFileService(this);
    }
}
