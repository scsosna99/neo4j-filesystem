package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
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
     * Persist file entry.  Very simple but pulled out in case future more needs to be done.
     * @param fsUri Neo4Jfs base URI
     * @param f file entry to persist
     * @return updated file entry
     */
    public FileEntry create(final URI fsUri,
                            final FileEntry f) {
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
