package dev.scottsosna.neo4jfs.filesystem;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.service.DirectoryService;
import dev.scottsosna.neo4jfs.util.SpringContext;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.NEO4JFS_PROPERTY_PAGINATION_SIZE;
import static java.util.Collections.emptyIterator;

/**
 * DirectoryStream implementation for Neo4J as defined by {@code java.nio.files.spi.FileSystemProvider}
 */
public class Neo4jfsDirectoryStream implements DirectoryStream<Path>, AutoCloseable {

    /**
     * No more work accomplished once the directory stream is closed.
     */
    private boolean closed = false;

    /**
     * Prevent unnecessary querying for additional subdirectories if we can detetermine we're done.
     */
    private boolean exhausted = false;

    /**
     * Directory for which subdirectories are being enumerated/streamed.
     */
    private final DirectoryEntry d;

    /**
     * Directory service provides children subdirectories of the starting directory.
     */
    private final DirectoryService service;

    private List<BaseEntry> children = List.of();

    /**
     * Pagination: tracks how many directories returned during previous queries that are skipped.
     */
    private int skippedCount = 0;

    /**
     * Neo4Jfs URI for current directory.
     */
    private final URI uri;

    /**
     * Pagination: number of children entries retrieved from Neo4J for each query.
     */
    private final int paginationMaxPerCall;

    /**
     * Constructor
     * @param path for which DirectoryStream is created
     */
    public Neo4jfsDirectoryStream(final Path path) throws IOException {
        this.service = SpringContext.getBean(DirectoryService.class);
        this.paginationMaxPerCall = SpringContext.getPropertyInteger(NEO4JFS_PROPERTY_PAGINATION_SIZE);

        //  Make sure the path exists.
        this.uri = path.toUri();
        List<BaseEntry> pathParts = service.find(path.toUri());
        if (pathParts == null || pathParts.isEmpty()) {
            throw new NoSuchFileException("%s: no such file or directory".formatted(path));
        }

        //  Make sure the path represents a directory.
        BaseEntry last = pathParts.getLast();
        if (last instanceof DirectoryEntry d) {
            this.d = d;
            queryForChildren();
        } else {
            throw new NotDirectoryException("%s: not a directory".formatted(path));
        }
    }

    /**
     *  Retrieve page worth of subdirectories from Neo4J.
     */
    private void queryForChildren() {
        if (!exhausted) {
            this.children = service.findChildren(uri, d, skippedCount, paginationMaxPerCall);
            this.exhausted = this.children == null || this.children.size() < paginationMaxPerCall;
            skippedCount += paginationMaxPerCall;
        }
    }

    /**
     * @return Directory stream for Neo4Jfs directory.
     */
    @Override
    public Iterator<Path> iterator() {
        return new Neo4jfsDirectoryIterator(this);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        exhausted = true;
    }

    /**
     * Internal class which iterates through subdirectories.
     */
    private class Neo4jfsDirectoryIterator implements Iterator<Path> {

        /**
         * Owning directory stream knows how to retrieve additional subdirectories for iterator.
         */
        private final Neo4jfsDirectoryStream ds;

        /**
         * Internal iterator for subdirectories.
         */
        private Iterator<BaseEntry> childIterator;

        /**
         * Constructor
         * @param ds owning Neo4jfsDirectoryStream
         */
        Neo4jfsDirectoryIterator(Neo4jfsDirectoryStream ds) {
            this.ds = ds;
            childIterator = ds.children != null && !ds.children.isEmpty() ? ds.children.iterator() : emptyIterator();
        }

        /**
         * Returns {@code true} if iterator has more elements (subdirectories).  Pagination may retrieve a subset,
         * therefore requiring additional queries to get more subdirs for this iterator.
         * @return true if more subdirectories to iterate through; false otherwise
         */
        @Override
        public boolean hasNext() {
            //  Iterator is invalid once the stream is closed.
            if (ds.closed) return false;

            //  Happy path: current iterator of subdirs has not been exhausted.
            if (childIterator.hasNext()) return true;

            //  Iterator exhausted, any reason to believe another query will return more?
            if (exhausted) return false;

            //  Possibly more, query and attempt to reload internal iterator.
            ds.queryForChildren();
            if (ds.d != null && !ds.children.isEmpty()) {
                //  More found, create new internal iterator.
                childIterator = ds.children.iterator();
                return childIterator.hasNext();
            }

            //  no more subdirectories found so sayonara.
            return false;
        }

        /**
         * @return path of next subdirectory
         */
        @Override
        public Path next() {
            if (uri.getPath().length() == 1) {
                return Path.of(URI.create("%s%s".formatted(ds.uri, childIterator.next().getName())));
            } else {
                return Path.of(URI.create("%s/%s".formatted(ds.uri, childIterator.next().getName())));
            }
        }

        /**
         * {@code remove} is not supported by this iterator.
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
