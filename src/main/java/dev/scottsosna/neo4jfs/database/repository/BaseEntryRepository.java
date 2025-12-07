package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;

import java.net.URI;

/**
 * Interface defining shared repository functionality.
 */
public interface BaseEntryRepository {

    /**
     * Attempt to find a child of a directory by name.  The child may be either a directory or a file (or truthfully
     * anything else) but the parent must be a directory.  The primary use case is to determine the existence/lack
     * thereof of a child for a specific name and, when present, its type.
     *
     * @param fsUri URI of the file system.
     * @param directoryNodeId generated node id for the parent directory
     * @param childNodeName name of the child node desired
     * @return the child node, when found, or null.
     */
    BaseEntry findNamedChild(final URI fsUri, final String directoryNodeId, final String childNodeName);

    /**
     * Save or update an entry to Neo4J
     *
     * @param uri URI of the file system, using host to identify database.
     * @param entry entry to be persisted
     * @param clazz specific class of entry, e.g. DirectoryEntry or FileEntry
     */
    <T extends BaseEntry> void save(final URI uri, final T entry, final Class<T> clazz);

    /**
     * Remove the relationship between two nodes identified by their ids.
     *
     * @param uri URI of the file system, using host to identify database.
     * @param startId start node from which outgoing relationship is to be removed
     * @param endId end node whose incoming relationship is to be removed
     */
    void deleteRelationship(final URI uri, final String startId, final String endId);

    /**
     * Only update last accessed timestamp for entry provided.  To guarantee inadvertent changes to entry aren't
     * accidentally persisted, the entry is loaded first and then updated
     *
     * @param uri URI of the file system, using host to identify database.
     * @param entry entry to have its last accessed timestamp updated
     */
    void updateLastAccessed(final URI uri, final BaseEntry entry, final Class<? extends BaseEntry> clazz);
}
