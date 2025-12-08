package dev.scottsosna.neo4jfs.database.node;

import java.util.ArrayList;

public class DirectoryBuilder {

    private boolean hidden = false;
    private boolean root = false;
    private String name;
    private String userName;
    private String groupName;

    /**
     * Default constructor.
     */
    public DirectoryBuilder() {
        //  Need empty constructor for Jackson, building root
    }

    /**
     * Constructor
     * @param parent parent directory of newly-created directory.
     */
    public DirectoryBuilder(final DirectoryEntry parent) {
        //  Certain values inherited from parent unless overridden.
        this.userName = parent.userName;
        this.groupName = parent.groupName;
    }

    /**
     * Creates new {@code DirectoryEntry} node and sets the properties as appropriate.
     * @return {@code DirectoryEntry} instance with properties set by builder
     */
    public DirectoryEntry build() {
        var dir = new DirectoryEntry();
        dir.name = name;
        dir.userName = userName;
        dir.groupName = groupName;
        dir.hidden = hidden;
        dir.root = root;
        dir.subdirs = new ArrayList<>();
        dir.files = new ArrayList<>();
        return dir;
    }

    /** -----------------------------------------------------------------------
     * Setters for mutable properties.
     * ------------------------------------------------------------------------ */

    public DirectoryBuilder hidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public DirectoryBuilder root(boolean root) {
        this.root = root;
        return this;
    }

    public DirectoryBuilder name(String name) {
        this.name = name;
        return this;
    }

    public DirectoryBuilder userName(String userName) {
        this.userName = userName;
        return this;
    }

    public DirectoryBuilder groupName(String groupName) {
        this.groupName = groupName;
        return this;
    }
}
