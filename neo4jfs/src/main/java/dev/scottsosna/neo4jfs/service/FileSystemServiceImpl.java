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

import dev.scottsosna.neo4jfs.database.model.neo4j.Database;
import dev.scottsosna.neo4jfs.database.model.neo4j.DatabaseAccessType;
import dev.scottsosna.neo4jfs.database.model.neo4j.DatabaseStatusType;
import dev.scottsosna.neo4jfs.database.model.neo4j.DatabaseType;
import dev.scottsosna.neo4jfs.database.repository.DatabaseRepository;
import dev.scottsosna.neo4jfs.exception.Neo4jfsDatabaseException;
import dev.scottsosna.neo4jfs.security.AccessManager;
import dev.scottsosna.neo4jfs.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;

/**
 * File service management service.
 */
@Service
public class FileSystemServiceImpl extends BaseNeo4jfsService implements FileSystemService {

    /**
     * Repository for working with Neo4J databases.
     */
    private final DatabaseRepository repository;

    /**
     * Directory services manages directories, its subdirs, and files.
     */
    private final DirectoryService directoryService;

    /**
     * Storage Manager persists the physical files of the file system.
     */
    private final StorageManager storageManager;

    /**
     * Logger for class.
     */
    private final static Logger logger = LoggerFactory.getLogger(FileSystemServiceImpl.class);

    /**
     * Constructor
     * @param repository Neo5J database repository
     * @param directoryService manages directories, subdirectories and files in file system.
     * @param storageManager persists physical files of the file system.
     * @param accessManager checks access permissions for service
     */
    public FileSystemServiceImpl(final DatabaseRepository repository,
                                 final DirectoryService directoryService,
                                 final StorageManager storageManager,
                                 final AccessManager accessManager) {
        super(accessManager);
        this.repository = repository;
        this.directoryService = directoryService;
        this.storageManager = storageManager;
    }

    /**
     * Creates new or validates existing file system, ensures root "directory" (node) exists and that
     * storage partition is available/usable.
     * @param fsUri Neo4Jfs URI for the file system to initialize.
     */
    public void init(final URI fsUri) throws IOException {
        //  Does a database exist for the partition?
        checkUri(fsUri);
        Database db = repository.find(fsUri);
        if (db != null) {
            //  Database exists, verify usability
            verifyDatabaseUsability(db);
            directoryService.findOrCreateRoot(fsUri);
        } else {
            //  No existing database, create new and add root directory.  Admin-only operation.
            checkAccessAdmin();
            repository.create(fsUri);
            directoryService.createRoot(fsUri);
        }

        //  Also make sure storage manager is available/initialized.
        storageManager.initPartition(fsUri);
    }

    /**
     * Deletes the complete file system, including content managed by Storage Manager.
     * @param fsUri Neo4Jfs URI for the file system to delete.
     */
    public void drop(final URI fsUri) throws IOException {
        //  Admin-only operation.
        checkAccessAdmin();

        //  Best effort made.  If Neo4J database fails to delete, no attempt to delete storage partition so the file
        //  system is still usable.  If Neo4J dataabase is deleted but storage partition fails to delete, the
        //  physical files are left dangling: unfortunate, but since Neo4J database is gone files aren't usable.
        //  Manual cleanup at a later date.
        checkUri(fsUri);
        try {
            repository.drop(fsUri);
            storageManager.dropPartition(fsUri);
        } catch (Exception e) {
            logger.warn("Unable to drop file system {}: {}", fsUri, e.getMessage());
        }
    }

    /**
     * The file system's {@code FileStore} instance is based on Storage Manager's partition
     * @param fsUri Neo4Jfs URI for the file system.
     * @return {@code FileStore} for the partition.
     * @throws IOException if an I/O error occurs.
     */
    public FileStore getFileStore(final URI fsUri) throws IOException {
        return storageManager.getPartitionFileStore(fsUri);
    }

    /**
     * The Neo4fJfs URI partition defined for an existing database which must meet requirements before use.
     * @param db Database to verify.
     */
    private void verifyDatabaseUsability(final Database db) throws IOException {
        if (db.getDefaultDatabase()) throw new Neo4jfsDatabaseException("%s: Partition database must not be default.".formatted(db.getName()));
        if (db.getType() == DatabaseType.SYSTEM) throw new Neo4jfsDatabaseException("%s: Partition database must not be system.".formatted(db.getName()));
        if (db.getAccess() != DatabaseAccessType.READ_WRITE) throw new Neo4jfsDatabaseException("%s: Partition database must be read-write.".formatted(db.getName()));
        if (db.getCurrentStatus() != DatabaseStatusType.ONLINE) throw new Neo4jfsDatabaseException("%s: Partition database must be online.".formatted(db.getName()));
    }
}
