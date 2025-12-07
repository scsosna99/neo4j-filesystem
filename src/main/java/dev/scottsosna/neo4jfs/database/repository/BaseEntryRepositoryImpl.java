package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import jakarta.annotation.PostConstruct;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

/**
 * Database functionality shared amongst all entry types.
 */
public class BaseEntryRepositoryImpl {

    //  Session factory for oft-use default database.
    /**
     * Session factor for oft-used default database.
     */
    protected static SessionFactory defaultSessionFactory;

    /**
     * Holds Neo4J connection and authentication credentials.
     */
    private Neo4jfsConfiguration config;

    /**
     * Session factories for each Neo4Jfs partition (database), static to allow sharing across services.
     */
    final private Map<String, SessionFactory> sessionFactories = new ConcurrentHashMap<>();

    /**
     * The "root" directory name (which, surprisingly, is ".").
     */
    protected static final String ROOT_DIRECTORY_NAME = Neo4jfsConstants.NAME_ROOT_DIRECTORY;

    /**
     * Various Cypher queries or chunks used for building complete query.
     */
    private static final String MATCH_ENTRY = "(d%d {name: $name%d})";
    private static final String MATCH_FILE = "(d%d:File {name: $name%d})";
    private static final String QUERY_DIRECTORY_AND_CHILD = "MATCH (p:Directory {id: $id})-[]->(c {name: $name}) RETURN p,c";
    private static final String QUERY_DELETE_NODE = "MATCH (n {id: $id}) DETACH DELETE n";
    private static final String QUERY_DELETE_RELATIONSHIP = "MATCH (start {id: $startId})-[r]-(end {id: $endId}) DELETE r";
    private static final String RELATIONSHIP_ENTRY = "-[]->";
    private static final String RELATIONSHIP_ENTRY_OPTIONAL = "OPTIONAL MATCH (d%d)-[]->";
    private static final String RELATIONSHIP_LINK_FILE = "-[:CONTAINS]->";
    private static final String RELATIONSHIP_LINK_FILE_OPTIONAL = "OPTIONAL MATCH (d%d)-[:CONTAINS]->";

    private static final Logger logger = LoggerFactory.getLogger(BaseEntryRepositoryImpl.class);

    /**
     * Constructor
     * @param config configuration bean hold Neo4J connection and authentication credentials.
     */
    public BaseEntryRepositoryImpl(Neo4jfsConfiguration config) {
        super();
        this.config = config;
    }

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
    public BaseEntry findNamedChild(final URI fsUri,
                                    final String directoryNodeId,
                                    final String childNodeName) {
        Map<String,Object> params = Map.of("id", directoryNodeId, "name", childNodeName);
        List<BaseEntry> entries = query(fsUri, QUERY_DIRECTORY_AND_CHILD, params, BaseEntry.class);
        return (entries.isEmpty() ? null : entries.get(1));
    }

    /**
     * Save or update an entry to Neo4J
     *
     * @param uri URI of the file system, using host to identify database.
     * @param entry entry to be persisted
     * @param clazz specific class of entry, e.g. DirectoryEntry or FileEntry
     */
    public <T extends BaseEntry> void save(final URI uri,
                                           final T entry,
                                           final Class<T> clazz) {
        updateTimestamps(entry);
        getSessionFactory(uri).openSession().save(entry);
    }

    /**
     * Remove the relationship between two nodes identified by their ids.
     *
     * @param uri URI of the file system, using host to identify database.
     * @param startId start node from which outgoing relationship is to be removed
     * @param endId end node whose incoming relationship is to be removed
     */
    public void deleteRelationship(final URI uri,
                                   final String startId,
                                   final String endId) {
        getSessionFactory(uri).openSession().query(QUERY_DELETE_RELATIONSHIP, Map.of("startId", startId, "endId", endId));
    }

    /**
     * Only update last accessed timestamp for entry provided.  To guarantee inadvertent changes to entry aren't
     * accidentally persisted, the entry is loaded first and then updated.  The update happens asynchronously as
     * it shouldn't require that anyone wait for it to complete.
     *
     * @param uri URI of the file system, using host to identify database.
     * @param entry entry to have its last accessed timestamp updated
     */
    @Async
    public void updateLastAccessed(final URI uri,
                                   final BaseEntry entry,
                                   final Class<? extends BaseEntry> clazz) {
        Session session = getSessionFactory(uri).openSession();
        BaseEntry loaded = session.load(clazz, entry.getId());
        loaded.setLastAccessed(Instant.now());
        session.save(loaded);
    }

    /**
     * Retrieve of create OGM session factory based on the URI.  Confirm that protocol is correct and
     * then use host as the database name.
     *
     * @param uri URI for the file system
     * @return session factory
     */
    protected SessionFactory getSessionFactory(final URI uri) {
        return sessionFactory(uri.getHost());
    }

    /**
     * Load an object from Neo4J by its id.
     *
     * @param uri URI for the file system
     * @param nodeId id of the node to load
     * @param clazz the class of the object to load
     * @return the loaded object
     */
    protected <T extends BaseEntry> T load(final URI uri,
                                           final String nodeId,
                                           final Class<T> clazz) {
        return getSessionFactory(uri).openSession().load(clazz, nodeId);
    }

    /**
     * Retrieve or create OGM session factory for the specified database.
     *
     * @param dbName database for which session factory is required
     * @return session factory for database.
     */
    private SessionFactory sessionFactory(final String dbName) {
        //  Has session factory been previously created?
        var sf = sessionFactories.get(dbName);
        if (sf == null) {
            //  no existing session factory so create new.
            sf = new SessionFactory(buildConfiguration(dbName),
                "dev.scottsosna.neo4jfs.database",
                "dev.scottsosna.neo4jfs.database.node");
            sessionFactories.put(dbName, sf);
        }

        return sf;
    }

    /**
     * Build configuration for connecting to Neo4J database.
     *
     * @param dbName database name to connect to
     * @return built configuration
     */
    private Configuration buildConfiguration(final String dbName) {
        return new Configuration.Builder()
            .uri(config.neo4jUri)
            .credentials(config.neo4jUsername, config.neo4jPassword)
            .database(dbName)
            .useNativeTypes()
            .build();
    }

    /**
     * Deletes entry - directory, file, etc. - by node ID.
     *
     * @param fsUri Neo4Jfs URI which identifies the partition (database)
     * @param nodeId identifies node to be deleted.
     * @return true if successfully deleted, false otherwise.
     */
    protected boolean deleteNodeById(final URI fsUri, final String nodeId) {
        Result r = getSessionFactory(fsUri).openSession().query(QUERY_DELETE_NODE, Map.of("id", nodeId));
        return r.queryStatistics().getNodesDeleted() > 0;
    }

    /**
     * Execute Cypher query and return objects found
     *
     * @param fsUri for the file system
     * @param query Cypher query to execute
     * @param parameters parameters to pass to Cypher query
     * @param clazz specific class of objects being returned
     * @return list of 0 or more objects of type clazz
     */
    protected <T> List<T> query(final URI fsUri,
                                final String query,
                                final Map<String,Object> parameters,
                                final Class<T> clazz) {
        var results = getSessionFactory(fsUri).openSession().query(clazz, query, parameters);
        return StreamSupport.stream(results.spliterator(), false).toList();
    }

    /**
     * Execute Cypher query and return result objects
     *
     * @param uri Neo4J URI identifying the partition (database)
     * @param query Cypher query to execute
     * @param clazz specific class of objects being returned
     * @return list of 0 or more objects of type clazz
     */
    protected <T> List<T> query(final URI uri,
                                final String query,
                                final Class<T> clazz) {
        return query(uri, query, Map.of(), clazz);
    }

    /**
     * Executes non-Cypher command where results are maps which require manual deserialization.
     * @param command Neo4J command to execute, e.g. SHOW DATABASE, CREATE DATABASE, etc.
     * @param parameters parameters used as substitution parameters
     * @return raw Neo4J OGM results
     */
    protected Result query(final String command, final Map<String,String> parameters) {
        return defaultSessionFactory.openSession().query(command, parameters);
    }

    /**
     * Executes non-Cypher query where results are maps which require manual deserialization.
     * @param command Neo4J command to execute, e.g. SHOW DATABASE, CREATE DATABASE, etc.
     * @return raw Neo4J OGM results.
     */
    protected Result query(final String command) {
        return query(command, Map.of());
    }

    /**
     * Extends Cypher query to navigate from a directory to any entry, such as a sub-directory or contained file
     * @param sbMatch builder for query
     * @param sbReturn builder for return clause
     * @param queryParams map of parameters to pass to Cypher query
     * @param entryName entry name to match
     * @param index depth of navigation, used for parameter names and return
     */
    protected void addMatchEntry(final StringBuilder sbMatch,
                                 final StringBuilder sbReturn,
                                 final Map<String,Object> queryParams,
                                 final String entryName,
                                 final int index) {
        addMatchWork(sbMatch, sbReturn, queryParams, entryName, index, RELATIONSHIP_ENTRY, MATCH_ENTRY);
    }

    /**
     * Extends Cypher query to navigate from a directory to any entry, such as a sub-directory or contained file
     * @param sbMatch builder for query
     * @param sbReturn builder for return clause
     * @param queryParams map of parameters to pass to Cypher query
     * @param entryName entry name to match
     * @param index depth of navigation, used for parameter names and return
     */
    protected void addMatchEntryOptional(final StringBuilder sbMatch,
                                         final StringBuilder sbReturn,
                                         final Map<String,Object> queryParams,
                                         final String entryName,
                                         final int index) {
        addMatchWork(sbMatch, sbReturn, queryParams, entryName, index, RELATIONSHIP_ENTRY_OPTIONAL, MATCH_ENTRY);
    }

    /**
     * Extends Cypher query to navigate from a directory to any entry which may or may not exists.
     * @param sbMatch builder for query
     * @param sbReturn builder for return clause
     * @param queryParams map of parameters to pass to Cypher query
     * @param entryName entry name to match
     * @param index depth of navigation, used for parameter names and return
     */
    protected void addMatchFile(final StringBuilder sbMatch,
                                final StringBuilder sbReturn,
                                final Map<String,Object> queryParams,
                                final String entryName,
                                final int index) {
        addMatchWork(sbMatch, sbReturn, queryParams, entryName, index, RELATIONSHIP_LINK_FILE, MATCH_FILE);
    }

    /**
     * Extends Cypher query to navigate from a directory to a file which may or may not exists.
     * @param sbMatch builder for query
     * @param sbReturn builder for return clause
     * @param queryParams map of parameters to pass to Cypher query
     * @param entryName entry name to match
     * @param index depth of navigation, used for parameter names and return
     */
    protected void addMatchFileOptional(final StringBuilder sbMatch,
                                        final StringBuilder sbReturn,
                                        final Map<String,Object> queryParams,
                                        final String entryName,
                                        final int index) {
        addMatchWork(sbMatch, sbReturn, queryParams, entryName, index, RELATIONSHIP_LINK_FILE_OPTIONAL, MATCH_FILE);
    }

    /**
     * Extends Cypher query to navigate from a directory to a file which may or may not exists.
     * @param sbMatch builder for query
     * @param sbReturn builder for return clause
     * @param queryParams map of parameters to pass to Cypher query
     * @param entryName entry name to match
     * @param index depth of navigation, used for parameter names and return
     * @param relationship Neo4J Cypher string for the relationship
     * @param endNode Neo4J Cypher string for the ending node
     */
    private void addMatchWork(final StringBuilder sbMatch,
                              final StringBuilder sbReturn,
                              final Map<String,Object> queryParams,
                              final String entryName,
                              final int index,
                              final String relationship,
                              final String endNode) {
        sbMatch
            //  needed for 'OPTIONAL MATCH' but not run of the mill so sometimes fomatting does nothing, looks weird
            .append(relationship.formatted(index - 1))
            .append(endNode.formatted(index, index));
        queryParams.put("name" + index, entryName);
        if (sbReturn != null) sbReturn.append(", d").append(index);
    }

    /**
     * Updates timestamps as appropriate for entry.  Entries persisted for first time will have no create timestamp,
     * therefore all timestamps set; otherwise just set last modified.
     * @param entry
     */
    private void updateTimestamps (final BaseEntry entry) {
        Instant now = Instant.now();
        if (entry.getCreated() == null) {
            entry.setCreated(now);
            entry.setLastAccessed(now);
            entry.setLastModified(now);
        } else {
            entry.setLastModified(now);
        }
    }

    /**
     * After component constructed, create the default session factory, used whenever a partition-specific
     * query is not required.
     */
    @PostConstruct
    private void init() {
        //  Get existing databases and look for one marked "default"
        defaultSessionFactory = sessionFactory(config.neo4jBaseDatabaseName);
    }
}
