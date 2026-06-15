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
package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Interface defining shared repository functionality.
 */
public interface BaseEntryRepository {

    /**
     * Attempt to find a child of a directory by name.  The child may be either a directory or a file (or truthfully
     * anything else) but the parent must be a directory.  The primary use case is to determine the existence/lack
     * thereof of a child for a specific name and, when present, its type.
     *
     * @param fsUri URI of the file system.
     * @param directoryNodeId generated node id for the parent directory
     * @param childNodeName name of the child node desired
     * @return the child node, when found, or null.
     */
    BaseEntry findNamedChild(final URI fsUri, final String directoryNodeId, final String childNodeName);

    /**
     * Save or update an entry to Neo4J
     *
     * @param uri URI of the file system, using host to identify database.
     * @param entry entry to be persisted
     * @param clazz specific class of entry, e.g. DirectoryEntry or FileEntry
     */
    <T extends BaseEntry> void save(final URI uri, final T entry, final Class<T> clazz);

    /**
     * Execute multiple database operations within single transaction.
     * @param fsUri URI for Neo4Jfs partion
     * @param tasks (hopefully) 2 or more database operations to execute
     * @throws IOException if I/O fails somehow
     */
     void save (final URI fsUri, final List<Callable> tasks) throws IOException;


    /**
     * Remove the relationship between two nodes identified by their ids.
     *
     * @param uri URI of the file system, using host to identify database.
     * @param startId start node from which outgoing relationship is to be removed
     * @param endId end node whose incoming relationship is to be removed
     * @return count of relationships deleted
     */
    Integer deleteRelationship(final URI uri, final String startId, final String endId);

    /**
     * Only update last accessed timestamp for entry provided.  To guarantee inadvertent changes to entry aren't
     * accidentally persisted, the entry is loaded first and then updated
     *
     * @param uri URI of the file system, using host to identify database.
     * @param entry entry to have its last accessed timestamp updated
     */
    void updateLastAccessed(final URI uri, final BaseEntry entry, final Class<? extends BaseEntry> clazz);
}
