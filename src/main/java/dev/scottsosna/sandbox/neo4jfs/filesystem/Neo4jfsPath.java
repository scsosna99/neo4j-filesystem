package dev.scottsosna.sandbox.neo4jfs.filesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;

public class Neo4jfsPath implements Path {

    //  The specific file system instance supporting this path.
    private final Neo4jFileSystem fs;

    //  The file path to be used for any operation on this path.
    private final String path;

    public Neo4jfsPath(FileSystem fs, String path) {
        this.fs = (Neo4jFileSystem) fs;
        this.path = path;
    }

    /**
     * @return file system associated with this path.
     */
    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Paths are always absolute, no concept of "current working directory"
     * @return {@code true}
     */
    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public Path getRoot() {
        return null;
    }

    @Override
    public Path getFileName() {
        return null;
    }

    @Override
    public Path getParent() {
        return null;
    }

    @Override
    public int getNameCount() {
        return 0;
    }

    @Override
    public Path getName(int index) {
        return null;
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return null;
    }

    @Override
    public boolean startsWith(Path other) {
        return false;
    }

    @Override
    public boolean endsWith(Path other) {
        return false;
    }

    @Override
    public Path normalize() {
        return null;
    }

    @Override
    public Path resolve(Path other) {
        return null;
    }

    @Override
    public Path relativize(Path other) {
        return null;
    }

    @Override
    public URI toUri() {
        return fs.getUri().resolve(path);
    }

    @Override
    public Path toAbsolutePath() {
        return null;
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return null;
    }

    @Override
    public int compareTo(Path other) {
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "";
    }
}
