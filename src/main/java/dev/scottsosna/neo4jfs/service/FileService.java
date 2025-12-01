package dev.scottsosna.neo4jfs.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;

public interface FileService  {
    void create (URI uri, InputStream is) throws IOException;
    void create (URI uri, Path sourceFile) throws IOException;
    void delete (URI uri) throws IOException;
    void delete (URI uri, String nodeId) throws IOException;
    InputStream getInputStream(URI uri) throws IOException;
    OutputStream getOutputStream(URI uri) throws IOException;
}
