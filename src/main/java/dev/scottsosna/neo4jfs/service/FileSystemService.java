package dev.scottsosna.neo4jfs.service;

import java.io.IOException;
import java.net.URI;

public interface FileSystemService {
    void init(URI uri) throws IOException;
    void drop(URI uri);
}