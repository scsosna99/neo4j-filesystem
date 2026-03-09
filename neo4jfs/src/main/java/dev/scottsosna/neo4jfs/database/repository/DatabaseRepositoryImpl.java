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
import dev.scottsosna.neo4jfs.database.model.neo4j.Database;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * Functionality for managing Neo4J databases.  Note that "databases" are not graph objects, so
 * commands not Cypher queries are used.
 */
@Component
public class DatabaseRepositoryImpl extends BaseEntryRepositoryImpl implements DatabaseRepository {

    /**
     * Neo4J commands for working with databases.
     */
    private static final String CREATE_DATABASE = "CREATE DATABASE $database";
    private static final String CREATE_INDEX = "CREATE INDEX %s IF NOT EXISTS FOR (%s) ON %s";
    private static final String DROP_DATABASE = "DROP DATABASE $database";
    private static final String SHOW_DATABASE = "SHOW DATABASE $database";
    private static final String SHOW_DATABASES = "SHOW DATABASES";
    private static final String PARAMETER_DATABASE = "database";

    /**
     * Constructor
     * @param config configuration bean hold Neo4J connection and authentication credentials.
     */
    public DatabaseRepositoryImpl(final Neo4jfsConfiguration config) {
        super(config);
    }

    /**
     * Package-private constructor for testing purposes
     * @param config configuration bean hold Neo4J connection and authentication credentials.
     * @param sessionFactory pre-configured session factory for testing
     */
    DatabaseRepositoryImpl(final Neo4jfsConfiguration config, final SessionFactory sessionFactory) {
        super(config, sessionFactory);
    }

    /**
     * Create new database based on URI
     *
     * @param fsUri Neo4Jfs file system URI for the database to be created
     * @return database just created
     */
    @Override
    public Database create(final URI fsUri) {
        //  Create the database
        String dbName = fsUri.getHost();
        query(CREATE_DATABASE, Map.of(PARAMETER_DATABASE, dbName));

        //  Create the indices
        createIndexes(fsUri);

        //  Return the newly-created database.
        return find(dbName);
    }

    /**
     * Create indexes to help performance
     * @param fsUri Neo4Jfs URI for database
     */
    public void createIndexes(final URI fsUri) {
        //  Note: Likely this needs to be more configurable than hard-coded, but good enough for now.  Also, I desperately
        //  tried to use substitution parameters but couldn't get it to work (other than index name).  Again, good enough.
        Session session = getSessionFactory(fsUri).openSession();
        session.query(CREATE_INDEX.formatted("ENTRY_NAME_IDX", "b:BaseEntry", "b.name"), Map.of());
        session.query(CREATE_INDEX.formatted("FILE_STORAGE_IDX", "f:File", "f.storageId"), Map.of());
    }

    /**
     * Drop an existing database from the Neo4J instance.
     *
     * @param fsUri Neo4J file system URI for the database to be dropped
     */
    public void drop(final URI fsUri) {
        query(DROP_DATABASE, Map.of(PARAMETER_DATABASE, fsUri.getHost()));
    }

    /**
     * Return a single database by, based on the "host" in the URI
     *
     * @param fsUri Neo4Jfs URI for database to be retrieved.
     * @return database found or null
     */
    @Override
    public Database find(final URI fsUri) {
        return find(fsUri.getHost());
    }

    /**
     * Return a single database by name
     *
     * @param dbName name of requested database
     * @return database found or null
     */
    @Override
    public Database find(final String dbName) {
        Result r = query(SHOW_DATABASE, Map.of(PARAMETER_DATABASE, dbName));
        List<Database> dbs = deserialize(r);
        return (dbs.isEmpty() ? null : dbs.getFirst());
    }

    /**
     * Return existing Neo4J databases
     *
     * @return list of daatabases currently in Neo4J instance.
     */
    @Override
    public List<Database> findAll() {
        Result r = query(SHOW_DATABASES);
        return deserialize(r);
    }

    /**
     * Take results of query and deserialize into Database objects.
     *
     * @param r Query results
     * @return list of zero or more Database objects
     */
    private List<Database> deserialize(final Result r) {
        return StreamSupport.stream(r.spliterator(), false)
            .map(Database::new)
            .toList();
    }
}
