package dev.scottsosna.sandbox.neo4jfs.service;

import dev.scottsosna.sandbox.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.sandbox.neo4jfs.database.model.storage.StorageFileInfo;
import dev.scottsosna.sandbox.neo4jfs.service.util.LocalStorageTreeDeleteVisitor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service("local")
public class LocalStorageManager implements StorageManager{

    //  The base directory where the Neo4Jfs files will be stored.
    @Value("${neo4jis.local.directory:#{null}}")
    private String neo4jfsBasePath;

    private final FileVisitor<Path> treeDeleteVisitor = new LocalStorageTreeDeleteVisitor();

    /**
     * Initialize a storage partition by ensuring directory exists.
     * @param uri
     */
    public void initPartition(URI uri) throws IOException {
        File partition = Path.of(neo4jfsBasePath, determinePartition(uri)).toFile();
        if (!partition.exists()) {
            partition.mkdirs();
        } else if (!partition.isDirectory()) {
            //  A "file" is not a directory and can't be used for storage.
            throw new RuntimeException("Partition exists but is not a directory: " + partition);
        }
    }

    /**
     * Neo4jFS being deleted, therefore delete all files in its partition
     * @param uri URI of Neo4Jfs filesystem
     */
    public void dropPartition(URI uri) throws IOException {
        Path partition = Path.of(neo4jfsBasePath, determinePartition(uri));
        try {
            Files.walkFileTree(partition, treeDeleteVisitor);
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete partition from local storage.", e);
        }
    }

    /**
     * Save the file to disk.
     * @param uri the URI for the Neo4Jfs file
     * @param is input stream for the file contents
     * @return file details, including storage id (relative path) and size
     */
    @Override
    public StorageFileInfo storeFile(URI uri, InputStream is) throws IOException {
        Path relativePath = generateRelativePath(uri);
        try {
            Path completePath = generateCompletePath(relativePath);
            verifySubdirectory(completePath);
            long bytes = Files.copy(is, completePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Saved " + bytes + " bytes to " + completePath);
            return new StorageFileInfo(relativePath.toString(), bytes);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save file to local storage.", e);
        }
    }

    /**
     * Save the file to disk.
     * @param uri URI for the Neo4Jfs file
     * @param sourceFile source file on local disk being stored in Neo4Jfs
     * @return file details, including storage id (relative path) and size
     */
    public StorageFileInfo storeFile(URI uri, File sourceFile) throws IOException {
        try {
            return storeFile(uri, Files.newInputStream(Path.of(sourceFile.getAbsolutePath())));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save file to local storage.", e);
        }
    }

    /**
     * Updates an existing file in Neo4Jfs. In an attempt to not lose data, the new version of the file
     * is saved before the existing is deleted.  This implies that the updated file has a new "storage id"
     * returned which is stored in Neo4jFS
     * @param uri Neo4Jfs filesystem URI
     * @param storageId the existing storage id to be replaced.
     * @param is from where data is streamed
     * @return file details, including storage id (relative path) and size
     */
    @Override
    public StorageFileInfo replaceFile(URI uri, String storageId, InputStream is) throws IOException {

        //  Save the new file
        StorageFileInfo info = storeFile(uri, is);

        //  Delete original after storing updated version, ignoring exceptions which don't really affect
        //  the overall results (other than dangling files left behind).
        try {
            deleteFile(storageId);
        } catch (Exception e) {
            System.out.println("Unable to delete replaced file.");
        }

        //  Return the new storage id
        return info;
    }

    /**
     * Updates an existing file in Neo4Jfs. In an attempt to not lose data, the new version of the file
     * is saved before the existing is deleted.  This implies that the updated file has a new "storage id"
     * returned which is stored in Neo4jFS
     * @param uri Neo4Jfs filesystem URI
     * @param storageId the existing storage id to be replaced.
     * @param sourceFile source file on local disk being stored in Neo4Jfs
     * @return file details, including storage id (relative path) and size
     */
    @Override
    public StorageFileInfo replaceFile(URI uri, String storageId, File sourceFile) throws IOException {
        try {
            return replaceFile(uri, storageId, Files.newInputStream(Path.of(sourceFile.getAbsolutePath())));
        } catch (IOException e) {
            throw new RuntimeException("Unable to updating file in local storage.", e);
        }
    }

    /**
     * Returns file details for a specific storage id.
     * @param uri Neo4jFS filesystem URI
     * @param storageId specifies file of interest
     * @return the file info
     */
    @Override
    public StorageFileInfo getFileInfo(URI uri, String storageId) {
        File file = generateCompletePath(Path.of(storageId)).toFile();
        return new StorageFileInfo(storageId, file.length());
    }

    /**
     * Create an OutputStream for requested file to allow caller to stream data to wherever
     * @param storageId file's relative pathname in Neo4Jfs filesystem
     * @return OutputStream to allow caller to retrieve data
     * @throws IOException thrown when file doesn't exist or is inaccessible.
     */
    public OutputStream getFile(String storageId) throws IOException {
        return Files.newOutputStream(generateCompletePath(Path.of(storageId)));
    }

    /**
     * Removes a specific file from Neo4Jfs
     * @param storageId the storage-specific identifier, in this case a relative path.
     */
    @Override
    public void deleteFile(String storageId) {
        try {
            Files.deleteIfExists(generateCompletePath(Path.of(storageId)));
        } catch (IOException e) {
            throw new RuntimeException("Unable to delete file from local storage.", e);
        }
    }

    /**
     * For local disk storage, partition equates with the Neo4J database, using URI's host as partition name.
     * @param uri Neo4jfs filesystem URI
     * @return partition name
     */
    private String determinePartition(URI uri) {
        return uri.getHost();
    }

    /**
     * The relative path is partition-specific, prepend the base pathname of the local storage manager which
     * represents the complete path of the soon-to-be-stored file.
     * @param relativePath partion and local file to be stored.
     * @return absolute path of the file to store
     */
    private Path generateCompletePath(Path relativePath) {
        return Path.of(neo4jfsBasePath, relativePath.toString());
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
        return Path.of(determinePartition(uri), uuid.substring(0, 2), uuid);
    }

    /**
     * Within each partition, the files are segregated into subdirectories to hopefully avoid too many files in a
     * single directory that would cause performance issues.  New subdirectories need to be created before
     * files are stored.
     * @param completePath destination pathname of the saved/updated file
     */
    private void verifySubdirectory(Path completePath) {
        File subdir = completePath.getParent().toFile();
        if (!subdir.exists()) {
            subdir.mkdir();
        } else if (!subdir.isDirectory()) {
            throw new RuntimeException("Subdirectory exists but is not a directory: " + subdir);
        }
    }

    /**
     * During startup, ensure the directory where files are stored exists.
     */
    @PostConstruct
    private void init() {
        //  Use current working directory as default when base path is not configured.
        if (neo4jfsBasePath == null) {
            neo4jfsBasePath = Path.of(System.getProperty("user.dir"), Neo4jfsConstants.NEO4JFS_URI_SCHEME).toString();
        }

        //  If base directory doesn't exist, create; if exists ensure it is a directory.
        File base = Path.of(neo4jfsBasePath).toFile();
        if (!base.exists()) {
            base.mkdirs();
        } else if (!base.isDirectory()) {
            throw new RuntimeException("Base path exists but is not a directory: " + neo4jfsBasePath);
        }
    }
}
