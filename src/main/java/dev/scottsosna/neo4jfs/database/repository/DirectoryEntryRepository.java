package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public interface DirectoryEntryRepository extends BaseEntryRepository {

    DirectoryEntry create (final URI fsUri, final DirectoryEntry toCreate, final DirectoryEntry parent);

    /**
     * Create new root directory for database/filesystem
     *
     * @param fsUri Neo4Jfs file system URI
     * @return {@link DirectoryEntry} for root directory.
     */
    DirectoryEntry createRoot(final URI fsUri);

    /**
     * Fetch the individual entries for a file or directory path which must exist.
     * @param uri fully-qualified Neo4Jfs file or directory URI
     * @return list of {@link BaseEntry} objects for the path or empty list if path doesn't exist
     */
    List<BaseEntry> find(final URI uri);

    /**
     * Fetch the individual entries for a file or directory path which must exist.
     * @param fsUri Neo4Jfs file system URI
     * @param path Neo4Jfs path to the specified file or directory
     * @return list of {@link BaseEntry} objects for the path or empty list if path doesn't exist
     */
    List<BaseEntry> find(final URI fsUri, final Path path);

    /**
     * Fetch the individual entries for a file or directory.  If the file/directory is optional, then the entries
     * for all parent directories are returned.  An empty list is returned if the parent directories don't exist.
     * @param fsUri Neo4Jfs file system URI
     * @param path Neo4Jfs path to the specified file or directory.
     * @param endNodeOptional indicates file or directory is optional.
     * @return list of {@link BaseEntry} objects for the file/directories or empty list if something doesn't exist.
     */
    List<BaseEntry> find(final URI fsUri, final Path path, final boolean endNodeOptional);

    /**
     * Fetch the individual entries for a file.  If the file is considered optional, then the entries
     * for all parent directories are returned.  An empty list is returned if the parent directories don't exist.
     * @param fsUri Neo4Jfs file system URI
     * @param path Neo4Jfs path to the specified file or directory.
     * @param endNodeOptional indicates file or directory is optional.
     * @return list of {@link BaseEntry} objects for the file/directories or empty list if something doesn't exist.
     */
    List<BaseEntry> findFile(final URI fsUri, final Path path, final boolean endNodeOptional);

    /**
     * Return the partition's root directory.
     * @param fsUri Neo4Jfs file system URI
     * @return root directory which, other than initial setup, must always exist.
     */
    DirectoryEntry findRoot(final URI fsUri);

    /**
     * Paginated retrieval of directory's children.
     * @param fsUri Neo4Jfs file system URI
     * @param directoryId Neo4J node ID of the target directory.
     * @param skip how many children to skip during pagination
     * @param limit maximum number of children to retrieve
     * @return updated {@code DirectoryEntry} with its subdirs and files or null if no children (remaining)
     */
    DirectoryEntry getChildren(final URI fsUri, final String directoryId, final int skip, final int limit);

    /**
     * Paginated retrieval files in a directory.
     * @param fsUri Neo4Jfs file system URI
     * @param directoryId Neo4J node ID of the target directory.
     * @param skip how many files to skip during pagination
     * @param limit maximum number of files to retrieve
     * @return updated {@code DirectoryEntry} with its files or null if no files (remaining).
     */
    List<FileEntry> getFiles(final URI fsUri, final String directoryId, final int skip, final int limit);

    /**
     * Paginated retrieval directory's subdirectories.
     * @param fsUri Neo4Jfs file system URI
     * @param directoryId Neo4J node ID of the target directory.
     * @param skip how many subdirs to skip during pagination
     * @param limit maximum number of subdirs to retrieve
     * @return updated {@code DirectoryEntry} with its subdirectories or null if no subdirectories (remaining).
     */
    List<DirectoryEntry> getSubdirs(final URI fsUri, final String directoryId, final int skip, final int limit);

    /**
     * Delete a directory entry from Neo4J.  NOTE: this is brute-force and does not check for existing subdirs/files.
     * @param fsUri Base URI for the file system.
     * @param directoryId Neo4J node ID of the directory entry to delete.
     * @return true if node deleted, false otherwise.
     */
    boolean delete(final URI fsUri, final String directoryId);

    /**
     * Retrieve parent directory for specified file/directory.
     * @param uri fully-qualified file/directory URI
     * @return parent directory of the URI.
     */
    BaseEntry parent(final URI uri);

    /**
     * Persist the updataed directory.
     * @param fsUri Neo4Jfs file system URI
     * @param d updated {@code DirectoryEntry} to persist.
     * @return updated object.
     */
    DirectoryEntry save(final URI fsUri, final DirectoryEntry d);

    /**
     * Determine whether a path exists.
     * @param uri fully-qualified file/directory URI
     * @return true if path exists, false otherwise.
     */
    boolean pathExists(final URI uri);
}
