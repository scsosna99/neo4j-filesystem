package dev.scottsosna.neo4jfs.storage.util;

import dev.scottsosna.neo4jfs.filesystem.Neo4jfsFileStore;
import dev.scottsosna.neo4jfs.storage.StorageManager;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Objects;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.ATTRIBUTE_VIEW_NAME_BASIC;

public class LocalStorageFileStore extends Neo4jfsFileStore {

    private final Path partitionPath;
    private LocalFileStoreAttributes attribs = null;

    /**
     * Constructor
     * @param partitionPath path to partition directory
     */
    public LocalStorageFileStore(Path partitionPath) {
        this.partitionPath = partitionPath;
    }

    /**
     * Returns the size, in bytes, of the file store.
     *
     * @return size in bytes
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long getTotalSpace() throws IOException {
        return getStoreAttributes().totalSpace();
    }

    /**
     * Returns the number of bytes that can be written to this file store.
     *
     * @return usable space in bytes
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long getUsableSpace() throws IOException {
        return getStoreAttributes().usableSpace();
    }

    /**
     * Returns the number of unallocated bytes in the file store.
     *
     * @return unallocated space in bytes
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long getUnallocatedSpace() throws IOException {
        return getStoreAttributes().unallocatedSpace();
    }

    /**
     * Tells whether or not this file store supports the file attribute view identified by the given type.
     *
     * @param type the file attribute view type
     * @return true if, and only if, the file attribute view is supported
     */
    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return false;
    }

    /**
     * Tells whether or not this file store supports the file attributes identified by the given file attribute view.

     * @param name the {@link FileAttributeView#name name} of file attribute view
     * @return {@code true} if, and only if, the file attribute view is supported
     */
    @Override
    public boolean supportsFileAttributeView(String name) {
        Objects.requireNonNull(name);
        return ATTRIBUTE_VIEW_NAME_BASIC.equals(name);
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        return null;
    }

    /**
     * Reads the value of a file store attribute.
     *
     * @param attribute the attribute to read
     * @return the attribute value
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Object getAttribute(String attribute) throws IOException {
        Objects.requireNonNull(attribute);
        if (attribute.equals("totalSpace"))
            return getTotalSpace();
        if (attribute.equals("usableSpace"))
            return getUsableSpace();
        if (attribute.equals("unallocatedSpace"))
            return getUnallocatedSpace();
        throw new UnsupportedOperationException("does not support the given attribute: " + attribute);
    }

    /**
     * Lazy-create the object that gets specific attributes from the file store.
     * @return LocalFileStoreAttributes instance
     * @throws IOException if an I/O error occurs
     */
    private LocalFileStoreAttributes getStoreAttributes() throws IOException {
        if (attribs == null) {
            attribs = new LocalFileStoreAttributes(partitionPath);
        }

        return attribs;
    }

    private final class LocalFileStoreAttributes {
        final FileStore fstore;
        final long size;

        LocalFileStoreAttributes(Path partitionPath) throws IOException {
            this.size = Files.size(partitionPath);
            this.fstore = Files.getFileStore(partitionPath);
        }

        long totalSpace() {
            return size;
        }

        long usableSpace() throws IOException {
            if (!fstore.isReadOnly())
                return fstore.getUsableSpace();
            return 0;
        }

        long unallocatedSpace()  throws IOException {
            if (!fstore.isReadOnly())
                return fstore.getUnallocatedSpace();
            return 0;
        }
    }
}
