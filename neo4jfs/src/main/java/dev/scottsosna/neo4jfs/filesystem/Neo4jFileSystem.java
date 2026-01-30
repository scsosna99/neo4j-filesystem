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
package dev.scottsosna.neo4jfs.filesystem;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

/**
 * Neo4Jfs file system implementation.
 */
public class Neo4jFileSystem extends FileSystem {

    /**
     * Is the file system considered open/usage?
     */
    private boolean isOpen = false;

    /**
     * Is the file system read-write or read-only?
     */
    private final boolean readOnly;

    /**
     * File store for the file system.
     */
    private final FileStore store;

    /**
     * Environment-specifics passed to file system provider when creating file system.
     */
    private final Map<String,?> env;

    /**
     * Root Neo4Jfs path for root for this file system.
     */
    private final Path rootPath;

    /**
     * Creating file system provider
     */
    private final Neo4jFileSystemProvider provider;

    /**
     * Pre-created set of root paths for this file system.
     */
    private final Set<Path> rootPaths;

    /**
     * Base file system URI in the form of "neo4jfs://partition/"
     */
    @Getter
    private final URI uri;

    /**
     * Constructor
     * @param provider creator of file system
     * @param fsUri Neo4Jfs URI for this file system
     * @param env any specific environment properties required/interpreted by provider
     */
    public Neo4jFileSystem(final Neo4jFileSystemProvider provider,
                           final URI fsUri,
                           final FileStore store,
                           final Map<String, ?> env) {
        this.provider = provider;
        this.uri = fsUri;
        this.store = store;
        this.env = (env != null) ? env : Map.of();
        isOpen = true;
        rootPath = new Neo4jfsPath(this, Neo4jfsConstants.NAME_ROOT_DIRECTORY);
        rootPaths = Set.of(rootPath);

        //  Determines if the file system was created/declared as read-only
        Object temp = env.get(Neo4jfsConstants.FILE_SYSTEM_ENV_READ_ONLY);
        readOnly = (temp != null) && Boolean.parseBoolean(temp.toString());
    }

    /**
     * @return the supporting provider for this file system/partition instance.
     */
    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    /**
     * MClose file system and remove from provider registry.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        if (!isOpen) return;
        isOpen = false;
        provider.removeFileSystem(uri);
    }

    /**
     * @return true if file system is open and usable, false otherwise.
     */
    @Override
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Neo4Jfs file systems/partitions are always read/write.
     * @return false
     */
    @Override
    public boolean isReadOnly() {
        return readOnly || store.isReadOnly();
    }

    /**
     * @return the path separator for his file system
     */
    @Override
    public String getSeparator() {
        return Neo4jfsConstants.PATH_SEPARATOR;
    }

    /**
     * @return set of root paths. Neo4Jfs has only one root path (e.g., no mountable volumes).
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        return rootPaths;
    }

    /**
     * TODO: need to figure out way of getting file store from storage manager.
     */
    @Override
    public Iterable<FileStore> getFileStores() {
        return null;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Neo4jfsConstants.SUPPORTED_ATTRIBUTE_VIEW_NAMES;
    }

    /**
     * Creates Neo4Jfs-specific path from the path string(s) passed in
     * @param first the path string or initial part of the path string
     * @param more additional strings to be joined to form the path string
     * @return new Neo4Jfs path
     */
    @Override
    public Path getPath(final String first, final String... more) {
        return new Neo4jfsPath(this, Path.of(first, more).toString());
    }

    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        return null;
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        return null;
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return null;
    }

    /**
     * @return root path for this file system
     */
    Path getRootPath() {
        return rootPath;
    }
}
