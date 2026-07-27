/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.filesystem.attribute;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * Neo4Jfs implementation of BasicFileAttributes using the file's/directory's basic information.
 */
public class BasicFileAttributesImpl implements BasicFileAttributes {

    /**
     * Underlying entry providing the attributes.
     */
    protected final BaseEntry entry;

    /**
     * Constructor
     * @param entry file/directory providing the attributes.
     */
    public BasicFileAttributesImpl(final BaseEntry entry) {
        this.entry = entry;
    }

    /**
     * Returns the time of last modification.
     * @return a {@code FileTime} representing the time of last modification
     */
    @Override
    public FileTime lastModifiedTime() {
        return FileTime.from(entry.getLastModified());
    }

    /**
     * Returns the time of last access.
     * @return a {@code FileTime} representing the time of last access
     */
    @Override
    public FileTime lastAccessTime() {
        return FileTime.from(entry.getLastAccessed());
    }

    /**
     * Returns the creation time.  The creation time is the time that the file was created.
     * @return a {@code FileTime} representing the creation time
     */
    @Override
    public FileTime creationTime() {
        return FileTime.from(entry.getCreated());
    }

    /**
     * Tells whether the file is a regular file with opaque content, i.e., a Neo4Jfs FileEntry.
     * @return true if the file is a regular file with opaque content, false otherwise
     */
    @Override
    public boolean isRegularFile() {
        return entry instanceof FileEntry;
    }

    /**
     * Tells whether the file is a directory, i.e., a Neo4Jfs DirectoryEntry.
     * @return true if the file is a directory, false otherwise
     */
    @Override
    public boolean isDirectory() {
        return entry instanceof DirectoryEntry;
    }

    /**
     * Tells whether the file is a synmolic line, which Neo4Jfs does not support.
     * @return false, Neo4Jfs does not support symbolic links.
     */
    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    /**
     * Tells whether the file is something other than a regular file, directory, or symbolic link,
     * which Neo4Jfs does not support.
     * @return false, Neo4Jfs does not anything other than files and directories.
     */
    @Override
    public boolean isOther() {
        return !isRegularFile() && !isDirectory() && !isSymbolicLink();
    }

    /**
     * @return the file size in bytes or 0 if the file is not a regular file
     */
    @Override
    public long size() {
        return isRegularFile() ? ((FileEntry) entry).getSize() : 0;
    }

    /**
     * @return an object (node ID) that uniquely identifies the given file or null.
     */
    @Override
    public Object fileKey() {
        return entry.getId();
    }
}
