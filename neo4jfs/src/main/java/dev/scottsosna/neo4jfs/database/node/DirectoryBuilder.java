/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.database.node;

import java.util.ArrayList;

public class DirectoryBuilder {

    private boolean hidden = false;
    private boolean root = false;
    private String name = null;
    private String permissions = null;
    private String groupName = null;
    private String userName = null;

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
        this.userName = parent.ownerUserName;
        this.groupName = parent.ownerGroupName;
    }

    /**
     * Creates new {@code DirectoryEntry} node and sets the properties as appropriate.
     * @return {@code DirectoryEntry} instance with properties set by builder
     */
    public DirectoryEntry build() {
        var dir = new DirectoryEntry();
        dir.name = name;
        dir.ownerUserName = userName;
        dir.ownerGroupName = groupName;
        dir.permissions = permissions;
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

    public DirectoryBuilder permissions(String permissions) {
        this.permissions = permissions;
        return this;
    }
}
