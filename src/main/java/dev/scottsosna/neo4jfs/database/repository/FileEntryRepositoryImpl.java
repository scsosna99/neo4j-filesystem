package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
import dev.scottsosna.neo4jfs.database.node.FileBuilder;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * {@link FileEntryRepository} interfacee for managing files in Neo4J.
 */
@Component
public class FileEntryRepositoryImpl extends BaseEntryRepositoryImpl implements FileEntryRepository {

    /**
     * Constructor
     * @param config N3o4Jfs configuration bean
     */
    public FileEntryRepositoryImpl(final Neo4jfsConfiguration config) {
        super(config);
    }

    /**
     * Create a new file entry
     * @param fsUri Neo4Jfs base URI
     * @param name file name
     * @param storageId ID for file as stored in Storage Manager
     * @param size size of the file.
     * @return persisted FileEntry
     */
    @Override
    public FileEntry create(final URI fsUri,
                            final String name,
                            final String storageId,
                            final long size) {

        //  Create the new file entry and persist.
        FileEntry f = new FileBuilder()
            .setName(name)
            .setStorageId(storageId)
            .setSize(size)
            .build();
        save(fsUri, f, FileEntry.class);

        return f;
    }

    /**
     * Delete a file entry by its Neo4J node ID
     * @param fsUri Neo4Jfs base URI
     * @param fileNodeId Neo4J node ID of the file entry to delete
     * @return true if deleted, false otherwise.
     */
    public boolean delete(final URI fsUri, final String fileNodeId) {
        return deleteNodeById(fsUri, fileNodeId);
    }

    /**
     * Load the specific file by its Neo4J node ID.
     * @param fsUri Neo4J base URI
     * @param fileNodeId Neo4J node ID of the file to load
     * @return FileEntry returned from database
     */
    public FileEntry load(final URI fsUri, final String fileNodeId) {
        return load(fsUri, fileNodeId, FileEntry.class);
    }
}
