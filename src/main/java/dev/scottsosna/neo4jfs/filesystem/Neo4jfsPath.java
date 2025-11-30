package dev.scottsosna.neo4jfs.filesystem;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Objects;

public class Neo4jfsPath implements Path {

    //  The specific file system instance supporting this path.
    private final Neo4jFileSystem fs;

    //  The file path to be used for any operation on this path.
    private final Path path;

    private final String pathString;

    /**
     * Constructor
     * @param fs Neo4jfs file system for this path.
     * @param path The detailed path within the file system.
     */
    public Neo4jfsPath(FileSystem fs, String path) {
        this.fs = (Neo4jFileSystem) fs;
        this.pathString = path;
        this.path = Path.of(path);
    }

    /**
     * @return file system associated with this path.
     */
    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Paths are always absolute, no concept of "current working directory" in Neo4j file system.
     * @return {@code true}
     */
    @Override
    public boolean isAbsolute() {
        return true;
    }

    /**
     * Neo4Jfs paths are absolute not relative therefore root is always "/"
     * @return Root path for file system
     */
    @Override
    public Path getRoot() {
        return fs.getRootPath();
    }

    @Override
    public Path getFileName() {
        if (path.getNameCount() > 0) {
            return new Neo4jfsPath(fs, path.getFileName().toString());
        } else {
            return new Neo4jfsPath(fs, Neo4jfsConstants.NAME_ROOT_DIRECTORY);
        }
    }

    @Override
    public Path getParent() {
        return new Neo4jfsPath(fs, path.getParent().toString());
    }

    @Override
    public int getNameCount() {
        return path.getNameCount();
    }

    @Override
    public Path getName(int index) {
        return new Neo4jfsPath(fs, path.getName(index).toString());
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        return new Neo4jfsPath(fs, path.subpath(beginIndex, endIndex).toString());
    }

    @Override
    public boolean startsWith(Path other) {
        return path.startsWith(other);
    }

    @Override
    public boolean endsWith(Path other) {
        return path.endsWith(other);
    }

    @Override
    public Path normalize() {
        return new Neo4jfsPath(fs, path.normalize().toString());
    }

    @Override
    public Path resolve(Path other) {
        return new Neo4jfsPath(fs, path.resolve(other).toString());
    }

    @Override
    public Path relativize(Path other) {
        return null;
    }

    @Override
    public URI toUri() {
        return fs.getUri().resolve(pathString);
    }

    @Override
    public Path toAbsolutePath() {
        return this;
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return this;
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
        return Objects.equals(this, other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fs, pathString);
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
