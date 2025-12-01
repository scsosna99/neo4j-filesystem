package dev.scottsosna.neo4jfs.storage;

import dev.scottsosna.neo4jfs.database.model.storage.StorageFileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public interface StorageManager {

    StorageFileInfo createFile(URI uri) throws IOException;
    StorageFileInfo createFile(URI uri, InputStream is) throws IOException;
    StorageFileInfo updateFile(URI uri, String storageId, InputStream is) throws IOException;
    StorageFileInfo getFileInfo(URI uri, String storageId);
    InputStream getFileInputStream(String storageId) throws IOException;
    OutputStream getFileOutputStream(String storageId) throws IOException;
    void deleteFile(String storageId);
    void dropPartition(URI uri) throws IOException;
    void initPartition(URI uri) throws IOException;
}
