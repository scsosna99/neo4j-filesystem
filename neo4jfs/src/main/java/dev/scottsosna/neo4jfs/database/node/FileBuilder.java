package dev.scottsosna.neo4jfs.database.node;

/**
 * {@code FileEntry} builder
 */
public class FileBuilder {

    boolean hidden = false;
    private String name;
    private String groupName;
    private String userName;
    private String storageId;
    private Long size;

    /**
     * Default, no-args constructor
     */
    public FileBuilder () {
        //  need default constructor
    }

    /**
     * Constructor
     * @param parent directory in which new file will be created.
     */
    public FileBuilder(final DirectoryEntry parent) {
        //  Certain values inherited from parent unless overridden.
        this.userName = parent.userName;
        this.groupName = parent.groupName;
    }

    /**
     * Creates new {@code FileEntry} node and sets the properties as appropriate.
     * @return {@code FileEntry} instance with properties set by builder
     */
    public FileEntry build() {
        FileEntry file = new FileEntry();
        file.name = name;
        file.userName = userName;
        file.groupName = groupName;
        file.hidden = hidden;
        file.storageId = storageId;
        file.size = size;
        file.hidden = false;
        return file;
    }

    /** -----------------------------------------------------------------------
     * Setters for mutable properties.
     * ------------------------------------------------------------------------ */

    public FileBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FileBuilder setStorageId(String storageId) {
        this.storageId = storageId;
        return this;
    }

    public FileBuilder setSize(Long size) {
        this.size = size;
        return this;
    }
}
