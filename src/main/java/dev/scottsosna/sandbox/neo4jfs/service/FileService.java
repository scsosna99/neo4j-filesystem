package dev.scottsosna.sandbox.neo4jfs.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface FileService  {
    void create (URI uri, InputStream inputStream) throws IOException;
    void create (URI uri, File sourceFile) throws IOException;
    void delete (URI uri) throws IOException;
    void delete (URI uri, String nodeId) throws IOException;
}
