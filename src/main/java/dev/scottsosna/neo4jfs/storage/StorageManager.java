package dev.scottsosna.neo4jfs.storage;

import dev.scottsosna.neo4jfs.database.model.storage.StorageFileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * Interface for storage managers implementation.
 *
 * Each Storage Manager is responsible for storing files within a Neo4J file system, be it local disk, cloud blob storage,
 * bit-chain protected storage or whatever other forms might be useful.
 */
public interface StorageManager {

    /**
     * A specific Neo4Jfs partition (instance) is being deleted, delete all files in partition
     * @param fsUri base Neo4Jfs URI
     */
    void dropPartition(URI fsUri) throws IOException;

    /**
     * Initializes partition for the file system specified by URI.
     * @param fsUri base Neo4Jfs URI
     */
    void initPartition(URI fsUri) throws IOException;

    /**
     * Copy existing file already managed by StorageManager, most likely due to file system copy.
     * @param fsUri base Neo4Jfs URI
     * @param storageId implementation-specific identifier for the file to be copied.
     * @return details for new file, including storage id (relative path) and size
     * @throws IOException file was unabled to be copied.
     */
    StorageFileInfo copyFile(URI fsUri, String storageId) throws IOException;

    /**
     * Creates empty Neo4Jfs file to be managed by Storage Manager
     * @param uri URI for the Neo4Jfs file
     * @return file details, including storage id (relative path)
     * @throws IOException unable to create file
     */
    StorageFileInfo createFile(URI uri) throws IOException;

    /**
     * Create new Neo4Jfs file to be managed by Storage Manager.
     * @param uri complete URI of the Neo4Jfs file
     * @param is input stream for the file contents
     * @return file details, including storage id (relative path) and size
     * @throws IOException unable to create/persist file
     */
    StorageFileInfo createFile(URI uri, InputStream is) throws IOException;

    /**
     * Delete file from storage manager, most likely because file deleted from Neo4Jfs filesystem.
     * @param fsUri base Neo4Jfs URI
     * @param storageId the storage-specific identifier, in this case a relative path.
     */
    void deleteFile(URI fsUri, String storageId) throws IOException;

    /**
     * Provide details about file managed by Storage Manager, based on storage id.
     * @param fsUri Neo4Jfs filesystem URI
     * @param storageId implementation-specific identifier for the file
     * @return file details, such as size.
     */
    StorageFileInfo getFileInfo(URI fsUri, String storageId) throws IOException;

    /**
     * Create input stream to allow file to be read
     * @param fsUri base Neo4Jfs URI
     * @param storageId implementation-specific identifier for the file
     * @return InputStream for reading data from file
     * @throws IOException thrown when file doesn't exist or is inaccessible.
     */
    InputStream getFileInputStream(URI fsUri, String storageId) throws IOException;

    /**
     * Create output stream to allow file to be written to.
     * @param uri Neo4J file URI
     * @param storageId implementation-specific identifier for the file
     * @return OutputStream for writing data to file
     * @throws IOException file doesn't exist, isn't accessible, isn't writeable
     */
    OutputStream getFileOutputStream(URI uri, String storageId) throws IOException;

    /**
     * Updates an existing file in Neo4Jfs.
     * To avoid losing data, the updated/replaced file is saved and then the existing is deleted.  A side-effect is
     * that the updated file has a new "storage id" which requires the owning FileEntry to be updated.
     * @param uri Neo4Jfs file URI
     * @param storageId implementation-specific identifier for the file to be updated
     * @param is from where data is streamed
     * @return details for updated file, including storage id (relative path) and size
     */
    StorageFileInfo updateFile(URI uri, String storageId, InputStream is) throws IOException;
}
