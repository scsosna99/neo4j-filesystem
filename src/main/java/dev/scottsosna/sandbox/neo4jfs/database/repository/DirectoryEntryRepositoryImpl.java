package dev.scottsosna.sandbox.neo4jfs.database.repository;

import dev.scottsosna.sandbox.neo4jfs.config.Neo4jfsConfiguration;
import dev.scottsosna.sandbox.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.sandbox.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.sandbox.neo4jfs.database.node.DirectoryBuilder;
import dev.scottsosna.sandbox.neo4jfs.database.node.DirectoryEntry;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Component
public class DirectoryEntryRepositoryImpl extends BaseEntryRepositoryImpl implements DirectoryEntryRepository {

    private static final String MATCH_ROOT = "(r:Directory {name: '/', root:true})";
    private static final String MATCH_DIRECTORY = "(d%d:Directory {name: $name%d, root: $root%d})";
    private static final String QUERY_CHILDREN_PAGINATED = "MATCH (p:Directory {id: $id})-[r*0..]->(c) RETURN p, r, c SKIP $skip LIMIT $limit";
    private static final String RELATIONSHIP_PARENT_OF = "-[:PARENT_OF]->";
    private static final String RELATIONSHIP_CONTAINS = "-[:CONTAINS]->";
    private static final Map<String,Object> MATCH_ROOT_PARAMS = Map.of("name0", Neo4jfsConstants.NAME_ROOT_DIRECTORY, "root0", Boolean.TRUE);

    public DirectoryEntryRepositoryImpl(Neo4jfsConfiguration config) {
        super(config);
    }

    public DirectoryEntry create(URI uri, String name) {
        DirectoryEntry d = new DirectoryBuilder()
            .name(name)
            .root(false)
            .build();
        save(uri, d, DirectoryEntry.class);
        return d;
    }

    public DirectoryEntry save(URI uri, DirectoryEntry d) {
        save(uri, d, DirectoryEntry.class);
        return d;
    }

    /**
     * Create new root directory for database/filesystem
     * @param uri URI for database/filesystem
     * @return root directory
     */
    public DirectoryEntry createRoot(URI uri) {
        DirectoryEntry d = new DirectoryBuilder()
            .name(ROOT_DIRECTORY_NAME)
            .userName(Neo4jfsConstants.NAME_ADMIN_USER)
            .groupName(Neo4jfsConstants.NAME_ADMIN_GROUP)
            .root(true)
            .build();
        getSessionFactory(uri).openSession().save(d);
        return d;
    }

    public DirectoryEntry findRoot(URI uri) {
        List<DirectoryEntry> d = query(uri, "MATCH(r:Directory {name: '/', root:true}) RETURN r", DirectoryEntry.class);
        return (d.isEmpty() ? null : d.getFirst());
    }

    public List<BaseEntry> find(URI uri, Path path) {
        return queryPath(uri, path);
    }

    public List<BaseEntry> find(URI uri) {
        return queryPath(uri);
    }

    public DirectoryEntry getParentWithChildren(URI uri, String parentId, int skip, int limit) {
        List<DirectoryEntry> results = query(uri, QUERY_CHILDREN_PAGINATED, Map.of("id", parentId, "skip", skip + 1, "limit", limit), DirectoryEntry.class);
        return results.isEmpty() ? null : results.getFirst();
    }

    public DirectoryEntry getParentWithChildren(URI uri, String parentId) {
        return getParentWithChildren(uri, parentId, 0, Integer.MAX_VALUE);
    }

    public BaseEntry parent(URI uri) {
        return queryLeaf(uri, Path.of(uri).getParent());
    }

    public boolean delete(URI uri, String fileNodeId) {
        return deleteNodeById(uri, fileNodeId);
    }

    private List<BaseEntry> queryPath(URI uri) {
        return queryPath(uri, Path.of(uri));
    }

    private List<BaseEntry> queryPathParent(URI uri) {
        return queryPath(uri, Path.of(uri).getParent());
    }

    private List<BaseEntry> queryPath(URI uri, Path path) {

        //  Break the URI's path into its constituent parts.
        List<String> paths = StreamSupport.stream(path.spliterator(), false).map(Path::toString).toList();

        //  Happy path: only a single path and its '/', meaning nothing specified other than host (file system), therefore
        //  just return the root node.
        if (paths.isEmpty() || (paths.size() == 1 && ROOT_DIRECTORY_NAME.equals(paths.getFirst()))) {
            return List.of(findRoot(uri));
        }

        //  Dynamically build cypher query that navigates from direcctory to directory.
        Map<String,Object> params = new HashMap<>(MATCH_ROOT_PARAMS);
        StringBuilder sbMatch = new StringBuilder("MATCH").append(MATCH_DIRECTORY.formatted(0, 0, 0));
        StringBuilder sbReturn = new StringBuilder(" RETURN d0");

        for (int i = 1; i < paths.size(); i++) {
            addMatchDirectory(sbMatch, sbReturn, params, paths.get(i - 1), false, i);
        }
        addMatchEntry(sbMatch, sbReturn, params, paths.getLast(), paths.size());

        //  Concatenate and query the whole thing
        return query(uri, sbMatch.toString() + sbReturn.toString(), params, BaseEntry.class);
    }

    private BaseEntry queryLeaf(URI uri, Path path) {

        //  Break the URI's path into its constituent parts.
        List<String> paths = StreamSupport.stream(path.spliterator(), false).map(Path::toString).toList();

        //  Happy path: only a single path and its '/', meaning nothing specified other than host (file system), therefore
        //  just return the root node.
        if (paths.isEmpty() || (paths.size() == 1 && paths.getFirst().equals(ROOT_DIRECTORY_NAME))) {
            return findRoot(uri);
        }

        //  Dynamically build cypher query that navigates from direcctory to directory.
        StringBuilder sbMatch = new StringBuilder("MATCH").append(MATCH_ROOT);
        int a = 1;
        for (String one: paths) {
            sbMatch
                .append("-[:PARENT_OF]->(d")
                .append(a)
                .append(":Directory {name: '")
                .append(one)
                .append("', root: false})");
            a++;
        }
        sbMatch.append("-[p*0..]->(c) RETURN d").append(a- 1).append(",p,c");

        //  Concatenate and query the whole thing
        List<DirectoryEntry> entries = query(uri, sbMatch.toString(), DirectoryEntry.class);
        return entries.isEmpty() ? null : entries.getFirst();
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
    private void addMatchDirectory(StringBuilder sbMatch,
                                   StringBuilder sbReturn,
                                   Map<String,Object> queryParams,
                                   String directoryName,
                                   Boolean isRoot,
                                   int index) {
        sbMatch
            .append(RELATIONSHIP_PARENT_OF)
            .append(MATCH_DIRECTORY.formatted(index, index, index));
        queryParams.put("name" + index, directoryName);
        queryParams.put("root" + index, isRoot);
        sbReturn.append(", d").append(index);
    }
}
