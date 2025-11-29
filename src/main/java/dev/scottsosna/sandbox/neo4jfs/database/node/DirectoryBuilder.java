package dev.scottsosna.sandbox.neo4jfs.database.node;

import java.util.ArrayList;

public class DirectoryBuilder {

    private boolean root = false;
    private String name;
    private String userName;
    private String groupName;

    public DirectoryEntry build() {
        var dir = new DirectoryEntry();
        dir.name = name;
        dir.userName = userName;
        dir.groupName = groupName;
        dir.root = root;
        dir.subdirs = new ArrayList<>();
        dir.files = new ArrayList<>();
        return dir;
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
