package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.model.storage.StorageFileInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public interface StorageManager {
    StorageFileInfo storeFile(URI uri, InputStream is) throws IOException;
    StorageFileInfo storeFile(URI uri, File sourceFile) throws IOException;
    StorageFileInfo replaceFile(URI uri, String id, InputStream is) throws IOException;
    StorageFileInfo replaceFile(URI uri, String storageId, File sourceFile) throws IOException;
    StorageFileInfo getFileInfo(URI uri, String storageId);
    OutputStream getFile(String storageId) throws IOException;
    void deleteFile(String storageId);
    void dropPartition(URI uri) throws IOException;
    void initPartition(URI uri) throws IOException;
}
