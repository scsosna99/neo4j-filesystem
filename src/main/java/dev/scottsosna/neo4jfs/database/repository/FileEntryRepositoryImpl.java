package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
import dev.scottsosna.neo4jfs.database.node.FileBuilder;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class FileEntryRepositoryImpl extends BaseEntryRepositoryImpl implements FileEntryRepository {

    public FileEntryRepositoryImpl(Neo4jfsConfiguration config) {
        super(config);
    }

    @Override
    public FileEntry create(URI uri, String name, String storageId, long size) {

        //  Create the new file entry and persist.
        FileEntry f = new FileBuilder()
            .setName(name)
            .setStorageId(storageId)
            .setSize(size)
            .build();
        save(uri, f, FileEntry.class);

        return f;
    }

    public boolean delete(URI uri, String fileNodeId) {
        return deleteNodeById(uri, fileNodeId);
    }

    public FileEntry load(URI uri, String fileNodeId) {
        return load(uri, fileNodeId, FileEntry.class);
    }
}
