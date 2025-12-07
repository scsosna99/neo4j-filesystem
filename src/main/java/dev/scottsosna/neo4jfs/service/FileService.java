package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

public interface FileService  {
    void copy (URI sourceUri, URI targetUri, final CopyOption... options) throws IOException;
    void copy (final FileEntry sourceFile, final URI targetUri, final DirectoryEntry targetDirectory, final CopyOption... options) throws IOException;
    void create (final URI uri, final InputStream is) throws IOException;
    void create (final URI uri, final Path sourceFile) throws IOException;
    void delete (final URI uri) throws IOException;
    void delete (final URI uri, final String nodeId) throws IOException;
    InputStream getInputStream(final URI uri) throws IOException;
    OutputStream getOutputStream(final URI uri) throws IOException;
    SeekableByteChannel newByteChannel(final URI uri, final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException;
}
