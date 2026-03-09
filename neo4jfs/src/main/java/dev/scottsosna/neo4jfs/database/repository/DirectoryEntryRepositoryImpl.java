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
import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryBuilder;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.database.repository.util.AddCypherClauseConsumer;
import jakarta.annotation.PostConstruct;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.*;

@Component
public class DirectoryEntryRepositoryImpl extends BaseEntryRepositoryImpl implements DirectoryEntryRepository {

    /**
     * For Neo4J OGM performance reasons, the Cypher queries use substitution parameters, therefore the query for any give path depth
     * is the same.  To avoid constant string building, we can prebuild the match and return strings and store them in simple structure
     * for easy access during query execution.
     */
    private String[] prebuiltQueryMatch;
    private String[] prebuiltQueryReturn;

    /**
     * How many prebuilt queries to generate at startup.
     */
    private static int PREBUILT_QUERY_SIZE = 20;

    /**
     * Various Cypher queries and clauses used during querying directory entries.
     */
    private static final String MATCH_DIRECTORY = "(d%d:Directory {name: $name%d, root: $root%d})";
    private static final String MATCH_ROOT = "(r:Directory {name: '/', root:true})";
    private static final Map<String,Object> MATCH_ROOT_PARAMS = Map.of("name0", Neo4jfsConstants.NAME_ROOT_DIRECTORY, "root0", Boolean.TRUE);
    private static final String QUERY_CHILDREN_PAGINATED = "MATCH (p:Directory {id: $id}) OPTIONAL MATCH(p)-[r]->(c) RETURN c SKIP $skip LIMIT $limit";
    private static final String QUERY_FILES_PAGINATED = "MATCH (p:Directory {id: $id}) OPTIONAL MATCH(p)-[r:CONTAINS]->(c:File) RETURN c SKIP $skip LIMIT $limit";
    private static final String QUERY_ROOT = "MATCH(r:Directory {name: '/', root:true}) RETURN r";
    private static final String QUERY_SUBDIRS_PAGINATED = "MATCH (p:Directory {id: $id}) OPTIONAL MATCH(p)-[r:PARENT_OF]->(c:Directory) RETURN c SKIP $skip LIMIT $limit";
    private static final String RELATIONSHIP_PARENT_OF = "-[:PARENT_OF]->";


    /**
     * Class logger
     */
    private static final Logger logger = LoggerFactory.getLogger(DirectoryEntryRepositoryImpl.class);

    /**
     * Constructor.
     * @param config Neo4Jfs configuration bean
     */
    public DirectoryEntryRepositoryImpl(Neo4jfsConfiguration config) {
        super(config);
    }

    /**
     * Package-private constructor for testing purposes
     * @param config Neo4Jfs configuration bean
     * @param sessionFactory pre-configured session factory for testing
     */
    DirectoryEntryRepositoryImpl(final Neo4jfsConfiguration config, final SessionFactory sessionFactory) {
        super(config, sessionFactory);
    }

    /**
     * Persist new directory to Neo4J and assign as subdir to parent directory.
     * @param fsUri Neo4Jfs file system URI
     * @param toCreate directory being created/persisted
     * @param parent parent directory of newly-created subdirectory
     * @return directory just created (e.g., it'll have a node id and timestamps updated)
     */
    public DirectoryEntry create (final URI fsUri,
                                  final DirectoryEntry toCreate,
                                  final DirectoryEntry parent) {
        save(fsUri, toCreate, DirectoryEntry.class);
        prepareEntry(toCreate, fsUri, parent);
        parent.setSubdirs(List.of(toCreate));
        save(fsUri, parent, DirectoryEntry.class);
        return toCreate;
    }

    /**
     * Create new root directory for database/filesystem
     * @param fsUri Neo4Jfs file system URI
     * @param adminUser admin/super-user for {@code AccessManager} implementation
     * @param adminGroup admin group for {@code AccessManager} implementation
     * @return {@link DirectoryEntry} for root directory.
     */
    public DirectoryEntry createRoot(final URI fsUri,
                                     final String adminUser,
                                     final String adminGroup) {

        //  Create new root with a bunch of default values.
        DirectoryEntry d = new DirectoryBuilder()
            .name(ROOT_DIRECTORY_NAME)
            .userName(adminUser)
            .groupName(adminGroup)
            .permissions(config.rootPermissions)
            .root(true)
            .build();

        //  Persist and "prepare" by adding transient values.
        save(fsUri, d, DirectoryEntry.class);
        prepareEntry(d, fsUri, null);

        return d;
    }

    /**
     * Fetch the individual entries for a file or directory path which must exist.
     * @param uri fully-qualified Neo4Jfs file or directory URI
     * @return list of {@link BaseEntry} objects for the path or empty list if path doesn't exist
     */
    public List<BaseEntry> find(final URI uri) {
        return find(uri, Path.of(uri), false);
    }

    /**
     * Fetch the individual entries for a file or directory path which must exist.
     * @param fsUri Neo4Jfs file system URI
     * @param path Neo4Jfs path to the specified file or directory
     * @return list of {@link BaseEntry} objects for the path or empty list if path doesn't exist
     */
    public List<BaseEntry> find(final URI fsUri, final Path path) {
        return find(fsUri, path, false);
    }

    /**
     * Fetch the individual entries for a file or directory.  If the file/directory is optional, then the entries
     * for all parent directories are returned.  An empty list is returned if the parent directories don't exist.
     * @param fsUri Neo4Jfs file system URI
     * @param path Neo4Jfs path to the specified file or directory.
     * @param endNodeOptional indicates file or directory is optional.
     * @return list of {@link BaseEntry} objects for the file/directories or empty list if something doesn't exist.
     */
    public List<BaseEntry> find(final URI fsUri,
                                final Path path,
                                final boolean endNodeOptional) {
        List<BaseEntry> entries = queryPath(fsUri, path, (endNodeOptional) ? this::addMatchEntryOptional : this::addMatchEntry);
        prepareEntriesTree(entries, fsUri);
        return entries;
    }

    /**
     * Fetch the individual entries for a directory path.  If the final directory is optional, then the entries
     * for all parent directories are returned.  An empty list is returned if the parent directories don't exist.
     * @param fsUri Neo4Jfs file system URI
     * @param path Neo4Jfs path to the specified file or directory.
     * @param endNodeOptional indicates file or directory is optional.
     * @return list of {@link BaseEntry} objects for the file/directories or empty list if something doesn't exist.
     */
    public List<BaseEntry> findDirectory(final URI fsUri,
                                         final Path path,
                                         final boolean endNodeOptional) {
        List<BaseEntry> entries = queryPath(fsUri, path, (endNodeOptional) ? this::addMatchDirectoryOptional : this::addMatchDirectory);
        prepareEntriesTree(entries, fsUri);
        return entries;
    }

    /**
     * Fetch the individual entries for a file.  If the file is considered optional, then the entries
     * for all parent directories are returned.  An empty list is returned if the parent directories don't exist.
     * @param fsUri Neo4Jfs file system URI
     * @param path Neo4Jfs path to the specified file or directory.
     * @param endNodeOptional indicates file or directory is optional.
     * @return list of {@link BaseEntry} objects for the file/directories or empty list if something doesn't exist.
     */
    public List<BaseEntry> findFile(final URI fsUri,
                                    final Path path,
                                    final boolean endNodeOptional) {
        List<BaseEntry> entries = queryPath(fsUri, path, (endNodeOptional) ? this::addMatchFileOptional : this::addMatchFile);
        prepareEntriesTree(entries, fsUri);
        return entries;
    }

    /**
     * Return the partition's root directory.
     * @param fsUri Neo4Jfs file system URI
     * @return root directory which, other than initial setup, must always exist.
     */
    public DirectoryEntry findRoot(final URI fsUri) {
        List<DirectoryEntry> d = query(fsUri, QUERY_ROOT, DirectoryEntry.class);
        return (d.isEmpty() ? null : prepareEntry (d.getFirst(), fsUri, null));
    }

    /**
     * Paginated retrieval of directory's children.
     * @param fsUri Neo4Jfs file system URI
     * @param parent specific directory for which children are returned
     * @param skip how many children to skip during pagination
     * @param limit maximum number of children to retrieve
     * @return list of BaseEntry for the children or an empty list.
     */
    public List<BaseEntry> getChildren(final URI fsUri,
                                       final DirectoryEntry parent,
                                       final int skip,
                                       final int limit) {
        return getSiblingsWork(fsUri, QUERY_CHILDREN_PAGINATED, parent, skip, limit, BaseEntry.class);
    }

    /**
     * Paginated retrieval files in a directory.
     * @param fsUri Neo4Jfs file system URI
     * @param parent specific directory for which children are returned
     * @param skip how many files to skip during pagination
     * @param limit maximum number of files to retrieve
     * @return updated {@code DirectoryEntry} with its files or null if no files (remaining).
     */
    public List<FileEntry> getFiles(final URI fsUri,
                                    final DirectoryEntry parent,
                                    final int skip,
                                    final int limit) {
        return getSiblingsWork(fsUri, QUERY_FILES_PAGINATED, parent, skip, limit, FileEntry.class);
    }

    /**
     * Paginated retrieval directory's subdirectories.
     * @param fsUri Neo4Jfs file system URI
     * @param parent specific directory for which sub-directories are returned
     * @param skip how many subdirs to skip during pagination
     * @param limit maximum number of subdirs to retrieve
     * @return updated {@code DirectoryEntry} with its subdirectories or null if no subdirectories (remaining).
     */
    public List<DirectoryEntry> getSubdirs(final URI fsUri,
                                           final DirectoryEntry parent,
                                           final int skip,
                                           final int limit) {
        return getSiblingsWork(fsUri, QUERY_SUBDIRS_PAGINATED, parent, skip, limit, DirectoryEntry.class);
    }

    /**
     * Delete a directory entry from Neo4J.  NOTE: this is brute-force and does not check for existing subdirs/files.
     * @param fsUri Base URI for the file system.
     * @param directoryId Neo4J node ID of the directory entry to delete.
     * @return true if node deleted, false otherwise.
     */
    public boolean delete(final URI fsUri, final String directoryId) {
        //  TODO: should we check for subdirs/files first?
        return deleteNodeById(fsUri, directoryId);
    }

    /**
     * Retrieve parent directory for specified file/directory.
     * @param uri fully-qualified file/directory URI
     * @return parent directory of the URI.
     */
    public BaseEntry parent(final URI uri) {
        return queryLeaf(uri, Path.of(uri).getParent());
    }

    /**
     * Persist the updataed directory.
     * @param fsUri Neo4Jfs file system URI
     * @param d updated {@code DirectoryEntry} to persist.
     * @return updated object.
     */
    public DirectoryEntry save(final URI fsUri, final DirectoryEntry d) {
        save(fsUri, d, DirectoryEntry.class);
        return d;
    }

    /**
     * Determine whether a path exists.
     * @param uri fully-qualified file/directory URI
     * @return true if path exists, false otherwise.
     */
    public boolean pathExists(final URI uri) {
        return !queryPath(uri).isEmpty();
    }

    /**
     * Does the actual work of returning siblings in a parent directory, files or directories or both based on query
     * @param fsUri Neo4Jfs file system URI
     * @param query Cypher query to execute
     * @param parent specific directory for which children are returned
     * @param skip how many children to skip during pagination
     * @param limit maximum number of children to retrieve
     * @return list of BaseEntry for the children or an empty list.
     */
    private <T extends BaseEntry> List<T> getSiblingsWork(final URI fsUri,
                                                          final String query,
                                                          final DirectoryEntry parent,
                                                          final int skip,
                                                          final int limit,
                                                          final Class<T> clazz) {
        List<T> results = query(fsUri, query,
            Map.of(CYPHER_PARAM_NODEID, parent.getId(),
                CYPHER_PARAM_PAGINATION_SKIP, skip,
                CYPHER_PARAM_PAGINATION_LIMIT, limit), clazz);
        prepareEntriesSiblings(results, fsUri, parent);
        return results;
    }


    /**
     * Query Neo4J for BaseEntry objects in the path specified.
     * @param uri fully-qualified file/directory URI
     * @return list of {@code BaseEntry} objects for the path or empty list if path doesn't exist.
     */
    private List<BaseEntry> queryPath(final URI uri) {
        return queryPath(uri, Path.of(uri));
    }

    /**
     * Query Neo4J for BaseEntry objects in the path specified.
     * @param fsUri Neo4Jfs file system URI
     * @param path Neo4Jfs path to the specified file or directory.
     * @return list of {@code BaseEntry} objects for the path or empty list if path doesn't exist.
     */
    private List<BaseEntry> queryPath(final URI fsUri, Path path) {
        return queryPath(fsUri, path, this::addMatchEntry);
    }

    /**
     * Dynamically builds Cypher query to return {@code BaseEntry}'s for the path specified.  The lastClause allows
     * customization: the leaf node is optional or is a file or whatever.
     * @param fsUri Neo4Jfs file system URI
     * @param path Neo4Jfs path to the specified file or directory.
     * @param lastClause how the leaf node is to be handled
     * @return list of {@code BaseEntry} objects for the path or empty list if path doesn't exist.
     */
    private List<BaseEntry> queryPath(final URI fsUri,
                                      final Path path,
                                      final AddCypherClauseConsumer lastClause) {

        //  Break the URI's path into its constituent parts.
        List<String> paths = StreamSupport.stream(path.spliterator(), false).map(Path::toString).toList();

        //  Happy path: only a single path and its '/', meaning nothing specified other than host (file system), therefore
        //  just return the root node.
        if (paths.isEmpty() || (paths.size() == 1 && ROOT_DIRECTORY_NAME.equals(paths.getFirst()))) {
            return List.of(findRoot(fsUri));
        }

        //  Build first part of Cypher query that navigates from direcctory to directory.
        Map<String,Object> params = new HashMap<>(MATCH_ROOT_PARAMS);
        StringBuilder sbMatch;
        StringBuilder sbReturn;

        //  The Cypher query submitted to Neo4J uses substitution parameters to improve performance, therefore we can
        //  prebuild queries to avoid repetitve building up of query through string concatenation.  If the number of
        //  paths is within the number of prebuilt queries, use the prebuilt to speed things up.
        int sizeMinusOne = paths.size() - 1;
        if (sizeMinusOne < PREBUILT_QUERY_SIZE) {
            //  Prebuilt query exists.
            sbMatch = new StringBuilder(prebuiltQueryMatch[sizeMinusOne]);
            sbReturn = new StringBuilder(prebuiltQueryReturn[sizeMinusOne]);
            for (int i = 1; i < paths.size(); i++) {
                params.put("name" + i, paths.get(i - 1));
                params.put("root" + i, Boolean.FALSE);
            }
        } else {
            //  Prebuilt query doesn't exist, so build it dynamically.  NOTE: yes, we could take use the largest
            //  prebuilt query and just append the remaining parts, but unlikely to see extreme path depth and
            //  therefore likely unnecessary at this time.  Maybe another day.
            sbMatch = new StringBuilder("MATCH").append(MATCH_DIRECTORY.formatted(0, 0, 0));
            sbReturn = new StringBuilder(" RETURN d0");
            for (int i = 1; i < paths.size(); i++) {
                addMatchDirectory(sbMatch, sbReturn, params, paths.get(i - 1), false, i);
            }
        }

        //  The final part of the Cypher query is dependent on what caller passed in
        lastClause.apply(sbMatch, sbReturn, params, paths.getLast(), paths.size());

        //  Concatenate and query the whole thing
        logger.debug("Path query: {}{}", sbMatch, sbReturn);
        return query(fsUri, sbMatch + sbReturn.toString(), params, BaseEntry.class);
    }

    /**
     * Return the leaf node of the path specified.
     * @param fsUri Neo4Jfs file system URI
     * @param path Neo4Jfs path to the specified file or directory.
     * @return the leaf node or null if path doesn't exist.
     */
    private BaseEntry queryLeaf(final URI fsUri, final Path path) {

        List<BaseEntry> entries = findDirectory(fsUri, path, false);
        if (!entries.isEmpty()) {
            prepareEntriesTree(entries, fsUri);
            return entries.getLast();
        } else {
            return null;
        }
    }

    /**
     * Extends Cypher query to navigate from a directory to a sub-directory through the PARENT_OF relationship.
     * @param sbMatch builder for query
     * @param sbReturn builder for return clause
     * @param queryParams map of parameters to pass to Cypher query
     * @param directoryName directory name to match
     * @param isRoot whether the directory is the root directory
     * @param index depth of navigation, used for parameter names and return
     */
    private void addMatchDirectory(final StringBuilder sbMatch,
                                   final StringBuilder sbReturn,
                                   final Map<String,Object> queryParams,
                                   final String directoryName,
                                   final Boolean isRoot,
                                   final int index) {
        sbMatch
            .append(RELATIONSHIP_PARENT_OF)
            .append(MATCH_DIRECTORY.formatted(index, index, index));
        queryParams.put("name" + index, directoryName);
        queryParams.put("root" + index, isRoot);
        sbReturn.append(", d").append(index);
    }

    /**
     * Prebuilds Cypher queries to minimize repeated string building for same thing.
     */
    @PostConstruct
    private void prebuildQueries() {
        prebuiltQueryMatch = new String[PREBUILT_QUERY_SIZE];
        prebuiltQueryReturn = new String[PREBUILT_QUERY_SIZE];

        //  Dynamically build cypher query that navigates from direcctory to directory.
        Map<String,Object> ignored = new HashMap<>();
        StringBuilder sbMatch = new StringBuilder("MATCH").append(MATCH_DIRECTORY.formatted(0, 0, 0));
        StringBuilder sbReturn = new StringBuilder(" RETURN d0");
        prebuiltQueryMatch[0] = sbMatch.toString();
        prebuiltQueryReturn[0] = sbReturn.toString();
        for (int i = 1; i < PREBUILT_QUERY_SIZE; i++) {
            addMatchDirectory(sbMatch, sbReturn, ignored, "", false, i);
            prebuiltQueryMatch[i] = sbMatch.toString();
            prebuiltQueryReturn[i] = sbReturn.toString();
        }
    }
}
