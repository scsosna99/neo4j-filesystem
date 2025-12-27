package dev.scottsosna.neo4jfs.filesystem.attribute;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.repository.DirectoryEntryRepository;
import dev.scottsosna.neo4jfs.util.SpringContext;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * Neo4Jfs implementation of BasicFileAttributeView.
 */
public class BasicFileAttributeViewImpl implements java.nio.file.attribute.BasicFileAttributeView {

    public static final String VIEW_NAME = "basic";

    /**
     * Underlying entry for file or directoryproviding the attributes.
     */
    protected final BaseEntry entry;

    /**
     * Repository for persisting attributes changes back to database.
     */
    private static DirectoryEntryRepository repository = null;


    /**
     * Constructor
     * @param entry file/directory providing the attributes.
     */
    public BasicFileAttributeViewImpl(BaseEntry entry) {
        this.entry = entry;
    }



    /**
     * Returns the name of the attribute view. Attribute views of this type have the name "basic".
     * @return name of the attribute view
     */
    @Override
    public String name() {
        return VIEW_NAME;
    }

    /**
     * Reads the basic file attributes as a bulk operation.
     * @return the file attributes
     * @throws IOException is an I/O error occurs
     */
    @Override
    public BasicFileAttributes readAttributes() throws IOException {
        return new BasicFileAttributesImpl( entry);
    }

    /**
     * Updates any or all of the file's last modified time, last access time, and create time attributes.
     *
     * @param lastModifiedTime the new last modified time, or {@code null} to not change the value
     * @param lastAccessTime the last access time, or {@code null} to not change the value
     * @param createTime the file's create time, or {@code null} to not change the value
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {

        boolean anythingChanged = false;
        if (lastModifiedTime != null) {
            entry.setLastModified(lastModifiedTime.toInstant());
            anythingChanged = true;
        }
        if (lastAccessTime != null) {
            entry.setLastAccessed(lastAccessTime.toInstant());
            anythingChanged = true;
        }
        if (createTime != null) {
            entry.setCreated(createTime.toInstant());
            anythingChanged = true;
        }

        if (anythingChanged) {
            persist();
        }
    }

    /**
     * Entry attributes have been updated, attempt to persist changes back to database
     */
    protected void persist() {
        //  This only works when the Neo4Jfs URI has been set on the enty.
        if (entry.getFsUri() != null) {
            DirectoryEntryRepository repo = getRepository();
            repo.save(entry.getFsUri(), entry, BaseEntry.class);
        } else {
            throw new UnsupportedOperationException("Entry has no URI: " + entry.getName());
        }
    }

    /**
     * @return {@code DirectoryEntryRepository} lazily retrieved from Spring context.
     */
    private DirectoryEntryRepository getRepository() {
        if (repository == null) {
            repository = SpringContext.getBean(DirectoryEntryRepository.class);
        }

        return repository;
    }
}
