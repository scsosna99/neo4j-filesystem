/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Licensed under the MIT license for non-commercial use.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 *
 * Licensed under the GPLv3 license for commercial use.  Please refer to LICENSE-GPL.md or
 * https://www.gnu.org/licenses/gpl-3.0.html for terms and conditions.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * expressed or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.scottsosna.neo4jfs.service.util;

import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.service.DirectoryService;
import dev.scottsosna.neo4jfs.util.SpringContext;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.NEO4JFS_PROPERTY_PAGINATION_SIZE;
import static java.util.Collections.emptyIterator;

/**
 * DirectoryStream implementation for Neo4J as defined by {@code java.nio.files.spi.FileSystemProvider}
 */
public class FileStream implements Iterable<FileEntry>, Closeable, AutoCloseable {

    /**
     * No more work accomplished once the directory stream is closed.
     */
    private boolean closed = false;

    /**
     * Prevent unnecessary querying for additional subdirectories if we can detetermine we're done.
     */
    private boolean exhausted = false;

    /**
     * Directory for which files are being enumerated/streamed.
     */
    private final DirectoryEntry d;

    /**
     * Directory service provides children subdirectories of the starting directory.
     */
    private final DirectoryService service;

    private List<FileEntry> files = List.of();

    /**
     * Pagination: tracks how many directories returned during previous queries that are skipped.
     */
    private int skippedCount = 0;

    /**
     * Neo4Jfs URI for current directory.
     */
    private final URI fsUri;

    /**
     * Pagination: number of children entries retrieved from Neo4J for each query.
     */
    private final int paginationMaxPerCall;

    public FileStream(final URI fsUri, final DirectoryEntry d) throws IOException {
        this.fsUri = fsUri;
        this.d = d;
        this.service = SpringContext.getBean(DirectoryService.class);
        this.paginationMaxPerCall = SpringContext.getPropertyInteger(NEO4JFS_PROPERTY_PAGINATION_SIZE);
    }

    /**
     *  Retrieve page worth of files from Neo4J.
     */
    private void queryForFiles() throws IOException {
        if (!exhausted) {
            this.files = service.findFiles(fsUri, d, skippedCount, paginationMaxPerCall);
            this.exhausted = this.files == null || this.files.size() < paginationMaxPerCall;
            skippedCount += paginationMaxPerCall;
        }
    }

    /**
     * @return Directory stream for Neo4Jfs directory.
     */
    @Override
    public Iterator<FileEntry> iterator() {
        return new FileIterator(this);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        exhausted = true;
    }

    /**
     * Internal class which iterates through files.
     */
    private class FileIterator implements Iterator<FileEntry> {

        /**
         * Owning file stream knows how to retrieve additional files for iterator.
         */
        private final FileStream ds;

        /**
         * Internal iterator for subdirectories.
         */
        private Iterator<FileEntry> fileIterator;

        /**
         * Constructor
         * @param ds owning Neo4jfsDirectoryStream
         */
        FileIterator(FileStream ds) {
            this.ds = ds;
            fileIterator = ds.d.getFiles() != null ? ds.d.getFiles().iterator() : emptyIterator();
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
            if (fileIterator.hasNext()) return true;

            //  Iterator exhausted, any reason to believe another query will return more?
            if (exhausted) return false;

            try {
                //  Possibly more, query and attempt to reload internal iterator.
                ds.queryForFiles();
                if (ds.files != null && !ds.files.isEmpty()) {
                    fileIterator = ds.files.iterator();
                    return fileIterator.hasNext();
                }
            } catch (IOException ioe) {
                //  Exception thrown when caller does not have EXECUTE permissions on directory.  Fall through
                //  and return false.
            }

            //  no more subdirectories found so sayonara.
            return false;
        }

        /**
         * @return entry for next file
         */
        @Override
        public FileEntry next() {
            return fileIterator.next();
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
