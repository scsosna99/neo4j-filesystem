package dev.scottsosna.neo4jfs.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;

public interface FileSystemService {
    void init(URI uri) throws IOException;
    void drop(URI uri);
    FileStore getFileStore(URI uri) throws IOException;
}