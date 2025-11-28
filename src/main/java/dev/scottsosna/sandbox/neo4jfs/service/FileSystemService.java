package dev.scottsosna.sandbox.neo4jfs.service;

import java.net.URI;

public interface FileSystemService {
    void init(URI uri);
    void drop(URI uri);
}