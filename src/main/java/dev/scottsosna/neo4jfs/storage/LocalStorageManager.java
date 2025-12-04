package dev.scottsosna.neo4jfs.storage;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.neo4jfs.database.model.storage.StorageFileInfo;
import dev.scottsosna.neo4jfs.exception.Neo4jfsNoSuchPartition;
import dev.scottsosna.neo4jfs.service.util.LocalStorageTreeDeleteVisitor;
import dev.scottsosna.neo4jfs.storage.util.CallbackOutputStream;
import dev.scottsosna.neo4jfs.storage.util.LocalStorageFileStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

/**
 * Storage Manager implementation that uses local disk for file management.
 * Each Neo4Jfs instance has its own "partition" - the URI host - to segregate and manage files separately, providing
 * protection from cross-instance/cross-partition file access/modifications.  The internal file names are random UUIDs.
 * For scaling/performance, files are stored in subdirectories based on the first two characters of the UUID.
 */
@Service("local")
public class LocalStorageManager implements StorageManager {

    /**
     * The base directory where Neo4Jfs files will be stored.
     */
    @Value("${neo4jis.local.directory:#{null}}")
    private String neo4jfsBasePath;

    /**
     * Visitor that handles deleting files/directories bottom up as tree is walked.
     */
    private final FileVisitor<Path> treeDeleteVisitor = new LocalStorageTreeDeleteVisitor();

    /**
     * Logger for this class.
     */
    private final static Logger logger = LoggerFactory.getLogger(LocalStorageManager.class);

    /**
     * Initializes partition for the file system specified by URI.
     * @param fsUri base Neo4Jfs URI
     */
    public void initPartition(URI fsUri) throws IOException {
        File partition = Path.of(neo4jfsBasePath, determinePartition(fsUri)).toFile();
        if (!partition.exists()) {
            partition.mkdirs();
        } else if (!partition.isDirectory()) {
            //  A "file" is not a directory and can't be used for storage.
            throw new NotDirectoryException(partition.toString());
        }
    }

    /**
     * A specific Neo4Jfs partition (instance) is being deleted, delete all files in partition
     * @param fsUri base Neo4Jfs URI
     */
    public void dropPartition(URI fsUri) throws IOException {
        Path partition = Path.of(neo4jfsBasePath, determinePartition(fsUri));
        Files.walkFileTree(partition, treeDeleteVisitor);
    }

    /**
     * Get the FileStore as supported by the Storage Manager implementation.
     *
     * @param fsUri base Neo4Jfs URI
     * @return FileStore for the partition.
     * @throws IOException partition directory does not exist.
     */
    public FileStore getPartitionFileStore(URI fsUri) throws IOException {
        String partitionName = determinePartition(fsUri);
        Path partitionPath = Path.of(neo4jfsBasePath, partitionName);
        if (partitionPath.toFile().exists()) {
            return new LocalStorageFileStore(partitionPath);
        } else {
            throw new Neo4jfsNoSuchPartition(partitionName);
        }
    }

    /**
     * Creates empty Neo4Jfs file to be managed by Storage Manager
     *
     * @param uri URI for the Neo4Jfs file
     * @return file details, including storage id (relative path)
     * @throws IOException unable to create file
     */
    @Override
    public StorageFileInfo createFile(URI uri) throws IOException {
        Path relativePath = generateRelativePath(uri);
        Path completePath = generateCompletePath(uri, relativePath);
        verifySubdirectory(completePath);
        Files.createFile(completePath);
        return new StorageFileInfo(relativePath.toString(), 0);
    }

    /**
     * Create new Neo4Jfs file to be managed by Storage Manager.
     *
     * @param uri complete URI of the Neo4Jfs file
     * @param is input stream for the file contents
     * @return file details, including storage id (relative path) and size
     * @throws IOException unable to create/persist file
     */
    @Override
    public StorageFileInfo createFile(URI uri, InputStream is) throws IOException {
        //  Create path, verify/create subdirectory.
        Path relativePath = generateRelativePath(uri);
        Path completePath = generateCompletePath(uri, relativePath);
        verifySubdirectory(completePath);

        //  Copy data from input stream to file.
        long bytes = Files.copy(is, completePath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Saved {} bytes to {}", bytes, completePath);
        return new StorageFileInfo(relativePath.toString(), bytes);
    }

    /**
     * Updates an existing file in Neo4Jfs.
     *
     * To avoid losing data, the updated/replaced file is saved and then the existing is deleted.  A side-effect is
     * that the updated file has a new "storage id" which requires the owning FileEntry to be updated.
     *
     * @param uri Neo4Jfs file URI
     * @param storageId implementation-specific identifier for the file to be updated
     * @param is from where data is streamed
     * @return details for updated file, including storage id (relative path) and size
     */
    @Override
    public StorageFileInfo updateFile(URI uri, String storageId, InputStream is) throws IOException {

        //  Save the new file
        StorageFileInfo info = createFile(uri, is);

        //  Delete original after storing updated version, ignoring exceptions which don't really affect
        //  the overall results (other than dangling files left behind).
        deleteFile(uri, storageId);

        //  Return the new storage id
        return info;
    }

    /**
     * Copy existing file already managed by StorageManager, most likely due to file system copy.
     *
     * @param fsUri base Neo4Jfs URI
     * @param storageId implementation-specific identifier for the file to be copied.
     * @return details for new file, including storage id (relative path) and size
     * @throws IOException file was unabled to be copied.
     */
    public StorageFileInfo copyFile(URI fsUri, String storageId) throws IOException{

        //  Create path for source (existing) and target (new) files.
        Path source = generateCompletePath(fsUri, Path.of(storageId));
        Path destination = generateCompletePath(fsUri, generateRelativePath(fsUri));
        verifySubdirectory(destination);

        //  Attempt to copy file
        Files.copy(source, destination);
        return getFileInfo(fsUri, destination.toString());
    }

    /**
     * Provide details about file managed by Storage Manager, based on storage id.
     *
     * @param fsUri Neo4Jfs filesystem URI
     * @param storageId implementation-specific identifier for the file
     * @return file details, such as size.
     */
    @Override
    public StorageFileInfo getFileInfo(URI fsUri, String storageId) throws IOException {
        File file = generateCompletePath(fsUri, Path.of(storageId)).toFile();
        return new StorageFileInfo(storageId, file.length());
    }

    /**
     * Create output stream to allow file to be written to.
     *
     * @param uri Neo4J file URI
     * @param storageId implementation-specific identifier for the file
     * @return OutputStream for writing data to file
     * @throws IOException file doesn't exist, isn't accessible, isn't writeable
     */
    @Override
    public OutputStream getFileOutputStream(URI uri, String storageId) throws IOException {
        return new CallbackOutputStream(Files.newOutputStream(generateCompletePath(uri, Path.of(storageId))));
    }

    /**
     * Creates {@code SeekableByteChannel} for the Neo4Jfs underlying file.
     *
     * @param fsUri base Neo4Jfs URI
     * @param storageId implementation-specific identifier for the file
     * @param options options for opening the file
     * @return {@code SeekableByteChannel} based on options passed in
     * @throws IOException error occurred opening file.
     */
    public SeekableByteChannel getSeekableByteChannel(URI fsUri, String storageId, Set<? extends OpenOption> options) throws IOException {
        return Files.newByteChannel(generateCompletePath(fsUri, Path.of(storageId)), options);
    }


    /**
     * Create input stream to allow file to be read
     *
     * @param uri Neo4J file URI
     * @param storageId implementation-specific identifier for the file
     * @return InputStream for reading data from file
     * @throws IOException thrown when file doesn't exist or is inaccessible.
     */
    @Override
    public InputStream getFileInputStream(URI uri, String storageId) throws IOException {
        return Files.newInputStream(generateCompletePath(uri, Path.of(storageId)));
    }

    /**
     * Delete file from storage manager, most likely because file deleted from Neo4Jfs filesystem.
     *
     * @param fsUri base Neo4Jfs URI
     * @param storageId the storage-specific identifier, in this case a relative path.
     */
    @Override
    public void deleteFile(URI fsUri, String storageId) throws IOException {
        Files.deleteIfExists(generateCompletePath(fsUri, Path.of(storageId)));
    }

    /**
     * For local disk storage, partition equates with the Neo4J database, using URI's host as partition name.
     *
     * @param uri Neo4Jfs filesystem URI
     * @return partition name
     */
    private String determinePartition(URI uri) {
        return uri.getHost();
    }

    /**
     * The relative path is partition-specific, prepend the base pathname of the local storage manager which
     * represents the complete path of the soon-to-be-stored file.
     *
     * @param fsUri base Neo4Jfs URI
     * @param relativePath partion and local file to be stored.
     * @return absolute path of the file to store
     */
    private Path generateCompletePath(URI fsUri, Path relativePath) {
        return Path.of(neo4jfsBasePath, determinePartition(fsUri), relativePath.toString());
    }

    /**
     * The Neo4Jfs relative path is determined by partition and generated UUID.  To prevent the partition directory
     * from too many entries the files are stored in subdirectories based on first two characters of UUID.
     *
     * @param uri Neo4J filesystem URI
     * @return the complete file path
     */
    private Path generateRelativePath(URI uri) {
        String uuid = UUID.randomUUID().toString();
        return Path.of(uuid.substring(0, 2), uuid);
    }

    /**
     * Within each partition, the files are segregated into subdirectories to hopefully avoid too many files in a
     * single directory that would cause performance issues.  New subdirectories need to be created before
     * files are stored.
     *
     * @param completePath destination pathname of the saved/updated file
     */
    private void verifySubdirectory(Path completePath) throws IOException {
        File subdir = completePath.getParent().toFile();
        if (!subdir.exists()) {
            subdir.mkdir();
        } else if (!subdir.isDirectory()) {
            throw new NotDirectoryException(subdir.getPath());
        }
    }

    /**
     * During startup, ensure the directory where files are stored exists.
     */
    @PostConstruct
    private void init() throws IOException{
        //  Use current working directory as default when base path is not configured.
        if (neo4jfsBasePath == null) {
            neo4jfsBasePath = Path.of(System.getProperty("user.dir"), Neo4jfsConstants.NEO4JFS_URI_SCHEME).toString();
        }

        //  If base directory doesn't exist, create; if exists ensure it is a directory.
        File base = Path.of(neo4jfsBasePath).toFile();
        if (!base.exists()) {
            base.mkdirs();
        } else if (!base.isDirectory()) {
            throw new NotDirectoryException(neo4jfsBasePath);
        }
    }
}
