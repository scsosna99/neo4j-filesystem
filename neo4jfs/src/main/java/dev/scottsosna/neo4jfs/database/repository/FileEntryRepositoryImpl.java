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

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * {@link FileEntryRepository} interfacee for managing files in Neo4J.
 */
@Component
public class FileEntryRepositoryImpl extends BaseEntryRepositoryImpl implements FileEntryRepository {

    /**
     * Various Cypher queries and clauses used during querying directory entries.
     */
    private static final String MATCH_STORAGE = "MATCH (f:File {storageId: $storageId}) RETURN f";

    /**
     * Constructor
     * @param config N3o4Jfs configuration bean
     */
    @Autowired
    public FileEntryRepositoryImpl(final Neo4jfsConfiguration config) {
        super(config);
    }

    /**
     * Package-private constructor for testing purposes
     * @param config N3o4Jfs configuration bean
     * @param sessionFactory pre-configured session factory for testing
     */
    FileEntryRepositoryImpl(final Neo4jfsConfiguration config, final SessionFactory sessionFactory) {
        super(config, sessionFactory);
    }

    /**
     * Persist file entry.  Very simple but pulled out in case future more needs to be done.
     * @param fsUri Neo4Jfs base URI
     * @param f file entry to persist
     * @return updated file entry
     */
    public FileEntry create(final URI fsUri,
                            final FileEntry f) {
        save(fsUri, f, FileEntry.class);
        return f;
    }

    /**
     * Delete a file entry by its Neo4J node ID
     * @param fsUri Neo4Jfs base URI
     * @param fileNodeId Neo4J node ID of the file entry to delete
     * @return true if deleted, false otherwise.
     */
    public boolean delete(final URI fsUri, final String fileNodeId) {
        return deleteNodeById(fsUri, fileNodeId);
    }

    /**
     * Load the specific file by its Neo4J node ID.
     * @param fsUri Neo4J base URI
     * @param fileNodeId Neo4J node ID of the file to load
     * @return FileEntry returned from database
     */
    public FileEntry load(final URI fsUri, final String fileNodeId) {
        return load(fsUri, fileNodeId, FileEntry.class);
    }

    /**
     * Find files associated with specified external storage ID
     * @param fsUri Neo4Jfs base URI
     * @param storageId external storage ID
     * @return List of files associated with storage ID
     */
    public List<FileEntry> findByStorageId(final URI fsUri, final String storageId) {
        return query(fsUri, MATCH_STORAGE, Map.of("storageId", storageId), FileEntry.class);
    }
}
