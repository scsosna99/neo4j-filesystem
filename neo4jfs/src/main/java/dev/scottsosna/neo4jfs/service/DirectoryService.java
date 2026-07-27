/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.filesystem.option.Neo4jfsDeleteOption;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.FileAttributeView;
import java.util.List;

/**
 * Interface for managing directories.
 */
public interface DirectoryService {

    /**
     * Checks the existence, and optionally the accessibility, of a file or directory.
     * @param path the path to the file/directory to check
     * @param modes the access modes to check; may have zero elements.
     * @throws IOException if access or I/O error occurs
     */
    void checkAccess(final Path path, final AccessMode... modes) throws IOException;

    /**
     * Each file system needs a root '/' directory that is (somewhat) immutable
     * @param fsUri Neo4Jfs URI for the file system (partition)
     * @return the newly-created root directory
     */
    DirectoryEntry createRoot (final URI fsUri) throws IOException;

    /**
     * Create file system root '/' directory if one doesn't already exist
     * @param fsUri Neo4Jfs URI for the file system (partition)
     * @return the root directory
     * @throws IOException if access or I/O error occurs
     */
    DirectoryEntry findOrCreateRoot(final URI fsUri) throws IOException;

    /**
     * Create new Neo4Jfs directory
     * @param uri fully-qualified Neo4Jfs URI specifying subdirectory to create
     * @return the newly created directory
     * @throws IOException I/O problem, such as parent directory doesn't exist.
     */
    DirectoryEntry mkdir (final URI uri) throws IOException;

    /**
     * Returns {@code BaseEntry} nodes representing path from root to specific directory/file.
     * @param uri fully-qualified Neo4Jfs URI specifying directory/file to find
     * @return {@code BaseEntry} list representing the target path or empty list if path doesn't exist.
     * @throws IOException if access not allowed on target
     */
    List<BaseEntry> find(final URI uri) throws IOException;

    /**
     * Return a directory with a paginated list of children (files, subdirectories)
     * @param fsUri Neo4Jfs base URI
     * @param parent specific directory for which children are returned
     * @param skip pagination: how many children skipped
     * @param limit pagination: how many children returned
     * @return list of BaseEntry for the children or an empty list.
     */
    List<BaseEntry> findChildren(final URI fsUri, final DirectoryEntry parent, final int skip, final int limit)  throws IOException;

    /**
     * Return a list of all subdirectories for the directory specified.
     * @param fsUri Neo4Jfs base URI
     * @param parent specific directory for which children are returned
     * @param skip pagination: how many children skipped
     * @param limit pagination: how many children returned
     * @return list of DirectoryEntry or an empty list.
     */
    List<DirectoryEntry> findSubdirs(final URI fsUri, final DirectoryEntry parent, final int skip, final int limit) throws IOException;

    /**
     * Copy file or directory to new location
     * @param sourceUri source file or directory to copy
     * @param fsUri target location
     * @param options copy options
     * @throws IOException if an I/O error occurs.
     */
    void copy(final URI sourceUri, final URI fsUri, final CopyOption... options) throws IOException;

    /**
     * Move file and directory within same file system.
     * @param sourceUri file or directory to move
     * @param targetUri target location.
     * @param options  options specifying how the move should be done
     * @throws IOException problems executing the move.
     */
    void move(final URI sourceUri, final URI targetUri, final CopyOption... options) throws IOException;

    /**
     * Return the parent directory of the specified URI
     * @param uri fully-qualified Neo4Jfs URI specifying directory/file to find parent of
     * @return pagent directory
     * @throws IOException thrown when user doesn't have access
     */
    BaseEntry parent(final URI uri) throws IOException;

    /**
     * Delete node specified by URI, file or directory
     * @param uri Neo4Jfs URI specifying either file or directory to delete.
     * @param options options specifying how to delete
     * @throws IOException I/O errors such file/directory doesn't exist or directory not empty.
     */
    void delete(final URI uri, final Neo4jfsDeleteOption... options) throws IOException;

    /**
     * Check whether directory/file specified exists,
     * @param uri Neo4Jfs URI for the directory or file to check
     * @return true if exists, false otherwise.
     */
    boolean exists(final URI uri);

    /**
     * Add persisted file (FileEntry node) to its parent directory (DirectoryEntry node)
     * @param fsUri Neo4Jfs file system URI
     * @param directory parent/containing directory of the file to add
     * @param fileToAdd file to add
     * @return updated DirectoryEntry
     */
    DirectoryEntry addFile(final URI fsUri, final DirectoryEntry directory, final FileEntry fileToAdd);

    /**
     * Return a directory with a paginated list of subdirectories
     * @param fsUri Neo4Jfs base URI
     * @parent specific directory for which files are returned
     * @param skip pagination: how many subdirs skipped
     * @param limit pagination: how many subdirs returned
     * @return updated {@code DirectoryEntry} with children collections
     */
    List<FileEntry> findFiles(final URI fsUri, final DirectoryEntry parent, final int skip, final int limit) throws IOException;

    /**
     * Returns the entry specified by URI as BasicFileAttributeView, needed by file system provider.
     * @param uri Neo4Jfs URI for the directory or file to read attribute view for.
     * @param clazz type of view to return.
     * @param options ignored
     * @return the attributes as a "view"
     * @throws IOException I/O error occurred while retrieving the entry to return
     */
    <T extends FileAttributeView> FileAttributeView readAttributeView(final URI uri, final Class<T> clazz, final LinkOption... options) throws IOException;

    /**
     * Attempt to set attribute with value provided
     * @param uri Neo4Jfs URI for directory/file to set attribute.
     * @param viewName name of view to modify
     * @param attribute attribute name to modify
     * @param value new attribute values
     * @param options options for any linked entries
     * @throws IOException if I/O error occurs
     */
    void setAttribute(final URI uri, final String viewName, final String attribute, final Object value, LinkOption... options) throws IOException;

    /**
     * Walks tree, dumping file structure to logger
     * @param uri Neo4Jfs URI for the directory to dump
     * @throws IOException I/O error occurred while walking tree
     */
    void dumpTree(final URI uri) throws IOException;

    /**
     * Avoids circular references between FileService and DirectoryService by having FileService register itself.
     * @param fs file service instance
     */
    void registerFileService(final FileService fs);

    /**
     * Some visitors are components which register themselves for simplified usage (e.g., no {@code new XYZFileVisitor()}
     * @param key unique identifier for visitor
     * @param visitor visitor instance.
     */
    void registerVisitor(final String key, final FileVisitor visitor);
}
