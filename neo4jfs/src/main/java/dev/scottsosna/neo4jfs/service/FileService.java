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
package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;

/**
 * Interface for managing files.
 */
public interface FileService  {
    /**
     * Create new, empty file for given URI
     * @param uri fully-qualified Neo4Jfs file URI
     * @throws IOException for whatever reason, the file could not be created
     */
    void create (final URI uri) throws IOException;

    /**
     * Copy the input stream into a new file
     * @param uri fully-qualified Neo4Jfs file URI
     * @param is input stream for reading file contents
     * @throws IOException if I/O fails during create for any reason.
     */
    void create (final URI uri, final InputStream is) throws IOException;

    /**
     * Helper method for creating/persisting local file into Neo4Jfs
     * @param uri Neo4Jfs file URI
     * @param sourceFile local file to persist
     * @throws IOException file cannot be created
     */
    void create (final URI uri, final Path sourceFile) throws IOException;

    /**
     * Copies source file to target directory or file
     * @param sourceUri Fully-qualified URI for source file
     * @param targetUri Fully-qualified URI for target file or directory.
     * @param options {@code CopyOption}s for this copy
     * @throws IOException any I/O error during copy
     */
    void copy (URI sourceUri, URI targetUri, final CopyOption... options) throws IOException;

    void copy (final FileEntry sourceFile, final URI targetUri, final DirectoryEntry targetDirectory, final CopyOption... options) throws IOException;

    /**
     * Delete a file by URI
     * @param uri fully-qualified URI for Neo4Jfs file
     * @throws IOException if I/O fails during delete.
     */
    void delete (final URI uri) throws IOException;

    /**
     * Delete a file by Neo4J node ID
     * @param fsUri Neo4Jfs files system URI
     * @param nodeId node ID of FileEntry to delete
     * @throws IOException thrown when delete fails, most like StorageManager but could be for other reasons
     */
    void delete (final URI fsUri, final String nodeId) throws IOException;

    /**
     * Create input stream for reading persisted file.
     * @param uri fully-qualified URI for file to read
     * @return InputStream for reading file contents
     * @throws IOException if I/O fails during read.
     */
    InputStream getInputStream(final URI uri) throws IOException;

    /**
     * Create output stream for writing to file persisted in Storage Manager
     * @param uri fully-qualified URI for file to write to
     * @return OutputStream for writing file contents
     * @throws IOException if I/O fails during write.
     */
    OutputStream getOutputStream(final URI uri) throws IOException;

    /**
     * Create new seekable byte channel for NeofJfs file
     * @param uri fully-qualified Neo4Jfs URI for file
     * @param options set of open options
     * @param attrs attributes for file
     * @return seekable byte channel
     * @throws IOException if I/O problem occurred
     */
    SeekableByteChannel newByteChannel(final URI uri, final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException;
}
