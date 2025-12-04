package dev.scottsosna.neo4jfs.filesystem;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;

import java.nio.file.FileStore;

abstract public class Neo4jfsFileStore extends FileStore {
    @Override
    public String name() {
        return "";
    }

    @Override
    public String type() {
        return Neo4jfsConstants.NEO4JFS_URI_SCHEME;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
