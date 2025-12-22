package dev.scottsosna.neo4jfs.filesystem;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Objects;

/**
 * Implementation of {@link java.nio.file.Path} for Neo4jfs.
 */
public class Neo4jfsPath implements Path {

    /**
     * The Neo4Jfs file system for this path, specifically the path.
     */
    private final Neo4jFileSystem fs;

    /**
     * The file's or directory's path represented as a straight Path (e.g., no scheme, no host/partition).
     */
    private final Path path;

    /**
     * The file's or directory's path initially passed in as a string.
     */
    private final String pathString;

    /**
     * Constructor
     * @param fs Neo4Jfs file system for this path.
     * @param path The detailed path within the file system.
     */
    public Neo4jfsPath(final FileSystem fs, final Path path) {
        this.fs = (Neo4jFileSystem) fs;
        this.path = path;
        this.pathString = path.toString();
    }

    /**
     * Constructor
     * @param fs Neo4Jfs file system for this path.
     * @param pathString The detailed path within the file system.
     */
    public Neo4jfsPath(final FileSystem fs, final String pathString) {
        this.fs = (Neo4jFileSystem) fs;
        this.pathString = pathString;
        this.path = Path.of(pathString);
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

    /**
     * Returns the name of the file or directory denoted by this path as a Path object. The file name is the farthest
     * element from the root in the directory hierarchy.
     * @return a path representing the name of the file or directory, or null if this path has zero elements
     */
    @Override
    public Path getFileName() {
        //  Explicitly handle root as it's not counted.
        if (path.getNameCount() > 0) {
            return path.getFileName();
        } else {
            return Path.of(Neo4jfsConstants.NAME_ROOT_DIRECTORY);
        }
    }

    /**
     * Returns the parent path, or null if this path does not have a parent.
     * @return a path representing the path's parent
     */
    @Override
    public Path getParent() {
        Path parent = path.getParent();
        return parent != null ? new Neo4jfsPath(fs, parent) : null;
    }

    /**
     * Returns the number of name elements in the path.
     * @return the number of elements in the path, or 0 if this path only represents a root component
     */
    @Override
    public int getNameCount() {
        return path.getNameCount();
    }

    /**
     * Returns a name element of this path as a Path object.
     * @param index the index of the element
     * @return the name element at the specified index
     */
    @Override
    public Path getName(final int index) {
        return path.getName(index);
    }

    /**
     * Returns a relative Path that is a subsequence of the name elements of this path.
     * @param beginIndex the index of the first element, inclusive
     * @param endIndex the index of the last element, exclusive
     * @return a new Path object that is a subsequence of the name elements in this Path
     */
    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        return new Neo4jfsPath(fs, path.subpath(beginIndex, endIndex).toString());
    }

    /**
     * Tests if this path starts with the given path.
     * @param other the given path
     * @return {@code true} if this path starts with the given path, {@code false} otherwise
     */
    @Override
    public boolean startsWith(final Path other) {
        return path.startsWith(other);
    }

    /**
     * Tests if this path ends with the given path.
     * @param other the given path
     * @return {@code true} if this path ends with the given path, {@code false} otherwise
     */
    @Override
    public boolean endsWith(final Path other) {
        return path.endsWith(other);
    }

    /**
     * Returns a path that is this path with redundant name elements eliminated.
     * @return a normalized path
     */
    @Override
    public Path normalize() {
        return new Neo4jfsPath(fs, path.normalize().toString());
    }

    /**
     * Converts a given path string to a Path and resolves it against this Path in exactly the manner specified
     * by the resolve method. For example, suppose that the name separator is "/" and a path represents "foo/bar",
     * then invoking this method with the path string "gus" will result in the Path "foo/bar/gus".
     * @param other the path to resolve against this path
     * @return the resulting path
     */
    @Override
    public Path resolve(final Path other) {
        switch (other) {
            case Neo4jfsPath npath:
                return new Neo4jfsPath(fs, path.resolve(npath.path));
            default:
                return path.resolve(other);
        }
    }

    /**
     * Constructs a relative path between this path and a given path.
     * @param other the path to relativize against this path
     * @return the resulting relative path, or an empty path if both paths are equal
     */
    @Override
    public Path relativize(final Path other) {
        switch (other) {
            case Neo4jfsPath npath:
                return new Neo4jfsPath(fs, path.relativize(npath.path));
            default:
                return path.relativize(other);
        }
    }

    /**
     * Returns a URI to represent this path.
     * @return the URI representing this path
     */
    @Override
    public URI toUri() {
        return fs.getUri().resolve(pathString);
    }

    /**
     * Returns a Path object representing the absolute path of this path.
     * @return a Path object representing the absolute path
     */
    @Override
    public Path toAbsolutePath() {
        return this;
    }

    /**
     * Returns a Path object representing the canonical form of this path.
     * @param options options indicating how symbolic links are handled
     * @return an absolute path representing the real path of the file located by this object
     * @throws IOException if the file does not exist or an I/O error occurs
     */
    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        return this;
    }

    /**
     * Registers the file located by this path with a watch service.
     * @param watcher the watch service to which this object is to be registered
     * @param events the events for which this object should be registered
     * @param modifiers the modifiers, if any, that modify how the object is registered
     * @return a key representing the registration of this object with the given watch service
     * @throws IOException if an I/O error occurs
     */
    @Override
    public WatchKey register(final WatchService watcher,
                             final WatchEvent.Kind<?>[] events,
                             final WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Compares two abstract paths lexicographically.
     * @param other the path compared to this path.
     * @return zero if the argument is equal to this path, a value less than zero if this path is lexicographically
     * less than the argument, or a value greater than zero if this path is lexicographically greater than the argument
     */
    @Override
    public int compareTo(final Path other) {
        return path.compareTo(other);
    }

    /**
     * Tests this path for equality with the given object.
     * @param other the object to which this object is to be compared
     * @return true if, and only if, the given object is a Path that is identical to this Path
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof Neo4jfsPath neo4jfsPath) {
            return  Objects.equals(this.fs, neo4jfsPath.fs) &&
                    Objects.equals(this.pathString, neo4jfsPath.pathString);
        } else {
            //  different types therefore not equal
            return false;
        }
    }

    /**
     * Computes a hash code for this path.
     * @return the hash-code value for this path
     */
    @Override
    public int hashCode() {
        return Objects.hash(fs, pathString);
    }

    /**
     * @return the string representation of this path
     */
    @Override
    public String toString() {
        return fs.getUri().resolve(pathString).toString();
    }
}
