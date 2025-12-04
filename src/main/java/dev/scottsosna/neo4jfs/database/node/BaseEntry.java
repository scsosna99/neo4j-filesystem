package dev.scottsosna.neo4jfs.database.node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.id.UuidStrategy;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

/**
 * Basic information required of all Neo4Jfs entries (files, directories, etc.)
 */
@Getter @Setter @NoArgsConstructor
public class BaseEntry implements BasicFileAttributes, BasicFileAttributeView {

    /**
     * Neo4J node identifier, auto-generated UUID.
     */
    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    String id;

    /**
     * Name of the entry
     */
    String name;

    /**
     * Username of creator, owner of the entry.
     */
    String userName;

    /**
     * Name of group associated with this entry, usually for security reasons.
     */
    String groupName;

    /**
     * Date/time when entity was created.
     */
    Instant created;

    /**
     * Date/time when entity was last modified.
     */
    Instant lastModified;

    /**
     * Date/time when entity was last accessed.
     */
    Instant lastAccessed;

    /**
     * Flag: is entry considered "hidden" when listing/walking directory contents?
     */
    boolean hidden;

    /** ----------------------------------------------------------------------------
     *  Following methods are defined in @code BasicFileAttributes
     *  ---------------------------------------------------------------------------- **/

    /**
     * Returns the time of last modification.
     * @return a {@code FileTime} representing the time of last modification
     */
    @Override
    public FileTime lastModifiedTime() {
        return FileTime.from(lastModified);
    }

    /**
     * Returns the time of last access.
     * @return a {@code FileTime} representing the time of last access
     */
    @Override
    public FileTime lastAccessTime() {
        return FileTime.from(lastAccessed);
    }

    /**
     * Returns the creation time.  The creation time is the time that the file was created.
     * @return a {@code FileTime} representing the creation time
     */
    @Override
    public FileTime creationTime() {
        return FileTime.from(created);
    }

    /**
     * Tells whether the file is a regular file with opaque content, i.e., a Neo4Jfs FileEntry.
     * @return true if the file is a regular file with opaque content, false otherwise
     */
    @Override
    public boolean isRegularFile() {
        return this instanceof FileEntry;
    }

    /**
     * Tells whether the file is a directory, i.e., a Neo4Jfs DirectoryEntry.
     * @return true if the file is a directory, false otherwise
     */
    @Override
    public boolean isDirectory() {
        return this instanceof DirectoryEntry;
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
        return isRegularFile() ? ((FileEntry) this).getSize() : 0;
    }

    /**
     * @return an object (node ID) that uniquely identifies the given file or null.
     */
    @Override
    public Object fileKey() {
        return id;
    }

    /** ----------------------------------------------------------------------------
     *  Following methods are defined in @code BasicFileAttributeView
     *  ---------------------------------------------------------------------------- **/
    /**
     * Returns the name of the attribute view. Attribute views of this type have the name "basic".
     * @return name of the attribute view
     */
    @Override
    public String name() {
        return "basic";
    }

    /**
     * Reads the basic file attributes as a bulk operation.
     * @return the file attributes
     * @throws IOException is an I/O error occurs
     */
    @Override
    public BasicFileAttributes readAttributes() throws IOException {
        return this;
    }

    /**
     * Updates any or all of the file's last modified time, last access time, and create time attributes.
     *
     * TODO: a {@code BaseEntry} is a Neo4J node and doesn't have a way to persist these changes back to
     * the database.  More thought needed, TBD whether there's a work-around or should in fact be a no-op.
     *
     * @param lastModifiedTime the new last modified time, or {@code null} to not change the value
     * @param lastAccessTime the last access time, or {@code null} to not change the value
     * @param createTime the file's create time, or {@code null} to not change the value
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        if (lastModifiedTime != null) {
            this.lastModified = lastModifiedTime.toInstant();
        }
        if (lastAccessTime != null) {
            this.lastAccessed = lastAccessTime.toInstant();
        }
        if (createTime != null) {
            this.created = createTime.toInstant();
        }
    }
}
