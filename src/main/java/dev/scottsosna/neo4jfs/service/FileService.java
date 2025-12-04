package dev.scottsosna.neo4jfs.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

public interface FileService  {
    void create (URI uri, InputStream is) throws IOException;
    void create (URI uri, Path sourceFile) throws IOException;
    void delete (URI uri) throws IOException;
    void delete (URI uri, String nodeId) throws IOException;
    InputStream getInputStream(URI uri) throws IOException;
    OutputStream getOutputStream(URI uri) throws IOException;
    SeekableByteChannel newByteChannel(URI uri, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException;
}
