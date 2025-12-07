package dev.scottsosna.neo4jfs.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;

public interface FileSystemService {
    void init(final URI uri) throws IOException;
    void drop(final URI uri);
    FileStore getFileStore(final URI uri) throws IOException;
}