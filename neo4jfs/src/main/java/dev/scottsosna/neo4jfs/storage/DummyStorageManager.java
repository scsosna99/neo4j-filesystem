/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.storage;

import dev.scottsosna.neo4jfs.database.model.storage.StorageFileInfo;
import dev.scottsosna.neo4jfs.storage.util.LocalStorageFileStore;
import dev.scottsosna.neo4jfs.storage.util.NullSeekableByteChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Dummy storage manager that emulates files stored externally without actually doing so.  Intended for development
 * when working on file tree functionality and actual files are unnecessary or bothersome.
 */
@Service
@ConditionalOnProperty(prefix = "neo4jfs", name = "storage", havingValue = "dummy")
public class DummyStorageManager implements StorageManager {

    Random random = new Random();

    public void initPartition(final URI fsUri) throws IOException {
    }

    public void dropPartition(final URI fsUri) throws IOException {
    }

    public FileStore getPartitionFileStore(final URI fsUri) throws IOException {
        return new LocalStorageFileStore(Path.of(System.getProperty("java.io.tmpdir")));
    }

    @Override
    public StorageFileInfo createFile(final URI uri) throws IOException {
        return new StorageFileInfo(generateRelativePath().toString(), 0);
    }

    @Override
    public StorageFileInfo createFile(final URI uri, final InputStream is) throws IOException {
        return new StorageFileInfo(generateRelativePath().toString(), random.nextInt(1024, 40960));
    }
    @Override
    public StorageFileInfo updateFile(final URI uri,
                                      final String storageId,
                                      final InputStream is) throws IOException {

        return new StorageFileInfo(generateRelativePath().toString(), random.nextInt(1024, 40960));
    }

    public StorageFileInfo copyFile(final URI fsUri, final String storageId) throws IOException{
        return getFileInfo(fsUri, generateRelativePath().toString());
    }

    @Override
    public StorageFileInfo getFileInfo(final URI fsUri, final String storageId) throws IOException {
        return new StorageFileInfo(storageId, random.nextInt(1024, 40960));
    }

    @Override
    public OutputStream getFileOutputStream(final URI uri, final String storageId) throws IOException {
        return OutputStream.nullOutputStream();
    }

    public SeekableByteChannel getSeekableByteChannel(final URI fsUri,
                                                      final String storageId,
                                                      final Set<? extends OpenOption> options) throws IOException {
        return new NullSeekableByteChannel();
    }

    @Override
    public InputStream getFileInputStream(final URI uri, final String storageId) throws IOException {
        byte[] bytes = new byte[random.nextInt(1024, 40960)];
        random.nextBytes(bytes);
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void deleteFile(final URI fsUri, final String storageId) throws IOException {
    }

    private Path generateRelativePath() {
        String uuid = UUID.randomUUID().toString();
        return Path.of(uuid.substring(0, 2), uuid);
    }
}
