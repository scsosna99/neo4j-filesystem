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
package dev.scottsosna.neo4jfs.integration;

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
import dev.scottsosna.neo4jfs.demo.DemoRunner;
import dev.scottsosna.neo4jfs.filesystem.attribute.GroupPrincipalImpl;
import dev.scottsosna.neo4jfs.filesystem.attribute.UserPrincipalImpl;
import dev.scottsosna.neo4jfs.security.AccessManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;

/**
 * Base class for all Neo4Jfs integration tests.  Each test gets a fresh Neo4J database:
 * {@link #setUpFileSystem()} creates the partition and {@link #tearDownFileSystem()} closes
 * then drops it, so every test starts with an empty graph.
 *
 * <p>Tests confirm correct behaviour through two lenses:
 * <ol>
 *   <li>Java NIO.2 – operations succeed or throw the expected exceptions.</li>
 *   <li>Neo4J – Cypher queries executed directly against the live graph verify that the
 *       correct nodes, relationships, and properties were written (or removed).</li>
 * </ol>
 *
 * <p>Storage is configured with {@code neo4jfs.storage=dummy} so no files are written to
 * disk; only the graph database is exercised.
 */
@SpringBootTest(
    classes = DemoRunner.class,
    properties = {"neo4jfs.storage=dummy"}
)
public abstract class AbstractNeo4jfsIntegrationTest {

    /** Name of the Neo4J database (= partition host) used by every test. */
    protected static final String TEST_PARTITION = "neo4jfs-test";

    /** Base URI for the test file system (must include root path for drop() / checkUri()). */
    protected static final URI TEST_FS_URI = URI.create("neo4jfs://" + TEST_PARTITION + "/");

    // -------------------------------------------------------------------------
    // Spring-managed beans injected into each test
    // -------------------------------------------------------------------------

    @Autowired
    protected AccessManager accessManager;

    @Autowired
    protected Neo4jfsConfiguration config;

    // -------------------------------------------------------------------------
    // Per-test state
    // -------------------------------------------------------------------------

    /** The live file system for this test; created in setUp and closed in tearDown. */
    protected FileSystem fileSystem;

    /** OGM session factory used exclusively for Cypher verification queries. */
    private SessionFactory verificationSessionFactory;

    // =========================================================================
    // JUnit lifecycle
    // =========================================================================

    /**
     * Opens a fresh Neo4Jfs file system for the test.  The security context is set to
     * the admin user so that initial directory/file creation by the test infrastructure
     * is always permitted.
     *
     * <p>A retry loop is included to handle the first-ever creation of the "neo4jfs-test"
     * Neo4J database: {@code CREATE DATABASE} is asynchronous, so the database may not
     * be immediately queryable by {@code createIndexes()}.  Retrying allows the database
     * time to come fully online.
     */
    @BeforeEach
    void setUpFileSystem() throws IOException {
        setSecurityContext(accessManager.getAdminUser(), accessManager.getAdminGroup());
        IOException lastException = null;
        for (int attempt = 1; attempt <= 10; attempt++) {
            try {
                fileSystem = FileSystems.newFileSystem(TEST_FS_URI, Map.of());
                return;
            } catch (IOException e) {
                lastException = e;
            }
            try { Thread.sleep(200L * attempt); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IOException("Failed to open test file system after 10 attempts", lastException);
    }

    /**
     * Closes the file system and wipes all graph nodes so each test starts with a
     * completely empty graph.
     *
     * <p>Rather than dropping and recreating the database (which triggers two bugs in the
     * current implementation — an async {@code CREATE DATABASE} race condition and a stale
     * static session-factory cache in {@code BaseEntryRepositoryImpl} — all nodes are
     * deleted with a Cypher {@code DETACH DELETE}.  This gives equivalent per-test isolation
     * without the timing issues.
     */
    @AfterEach
    void tearDownFileSystem() throws IOException {
        setSecurityContext(accessManager.getAdminUser(), accessManager.getAdminGroup());

        if (fileSystem != null && fileSystem.isOpen()) {
            fileSystem.close();
        }

        // Wipe all graph nodes.  Silently ignore errors when the database does not exist
        // (e.g. if setUp itself failed before creating it).
        try {
            openVerificationSession().query("MATCH (n) DETACH DELETE n", Map.of());
        } catch (Exception ignored) {
            // Database may not exist yet on first run; next setUp will create it.
        }

        if (verificationSessionFactory != null) {
            verificationSessionFactory.close();
            verificationSessionFactory = null;
        }
    }

    // =========================================================================
    // Security helpers
    // =========================================================================

    /**
     * Replaces the current Spring Security context with a simple token for the
     * given user / group pair, matching the pattern used by the demo programs.
     *
     * @param userName  principal name
     * @param groupName single authority / group granted to the principal
     */
    protected void setSecurityContext(final String userName, final String groupName) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(
            new TestingAuthenticationToken(
                userName,
                "integration-test",
                List.of(new SimpleGrantedAuthority(groupName))));
        SecurityContextHolder.setContext(ctx);
    }

    // =========================================================================
    // File-system setup helpers
    // =========================================================================

    /**
     * Creates a directory and immediately transfers ownership and permissions to
     * the specified user/group.  Must be called while an admin security context is
     * active (the caller is responsible for switching contexts).
     *
     * @param path        directory path to create
     * @param owner       new owning user
     * @param group       new owning group
     * @param permissions POSIX permission set
     */
    protected void createOwnedDirectory(final Path path,
                                        final String owner,
                                        final String group,
                                        final EnumSet<PosixFilePermission> permissions) throws IOException {
        Files.createDirectory(path);
        Files.setOwner(path, new UserPrincipalImpl(owner));
        Files.getFileAttributeView(path, PosixFileAttributeView.class).setGroup(new GroupPrincipalImpl(group));
        Files.setPosixFilePermissions(path, permissions);
    }

    /**
     * Creates a file and immediately sets explicit permissions on it.
     * Must be called while a security context that has write access to the parent
     * directory is active.
     *
     * @param path        file path to create
     * @param permissions POSIX permission set
     */
    protected void createFileWithPermissions(final Path path,
                                             final EnumSet<PosixFilePermission> permissions) throws IOException {
        Files.createFile(path);
        Files.setPosixFilePermissions(path, permissions);
    }

    // =========================================================================
    // Neo4J verification helpers – raw Cypher against the live graph
    // =========================================================================

    /**
     * Executes a Cypher query against the test partition database.
     * Each call opens a fresh OGM session so that cached state never masks changes
     * made by the file-system operations under test.
     *
     * @param cypher Cypher query string
     * @param params query parameters
     * @return raw OGM result rows
     */
    protected Iterable<Map<String, Object>> runCypher(final String cypher,
                                                       final Map<String, Object> params) {
        return openVerificationSession().query(cypher, params);
    }

    /**
     * Convenience overload for parameter-free queries.
     */
    protected Iterable<Map<String, Object>> runCypher(final String cypher) {
        return runCypher(cypher, Map.of());
    }

    /**
     * Returns {@code true} when at least one node with the given label and name
     * exists in the test partition database.
     *
     * @param label Neo4J node label, e.g. {@code "File"} or {@code "Directory"}
     * @param name  value of the node's {@code name} property
     */
    protected boolean nodeExistsInGraph(final String label, final String name) {
        String cypher = "MATCH (n:" + label + " {name: $name}) RETURN n LIMIT 1";
        return runCypher(cypher, Map.of("name", name)).iterator().hasNext();
    }

    /**
     * Returns the count of nodes with the given label and name.
     */
    protected long countNodesInGraph(final String label, final String name) {
        String cypher = "MATCH (n:" + label + " {name: $name}) RETURN count(n) AS cnt";
        var it = runCypher(cypher, Map.of("name", name)).iterator();
        if (!it.hasNext()) return 0L;
        Object val = it.next().get("cnt");
        return val instanceof Long l ? l : ((Number) val).longValue();
    }

    /**
     * Returns the value of a single scalar property for the first node that matches
     * the given label and name, or {@code null} when no matching node is found.
     *
     * @param label    Neo4J node label
     * @param name     node name
     * @param property property to retrieve
     */
    protected Object getNodeProperty(final String label,
                                     final String name,
                                     final String property) {
        String cypher = "MATCH (n:" + label + " {name: $name}) RETURN n." + property + " AS value LIMIT 1";
        var it = runCypher(cypher, Map.of("name", name)).iterator();
        return it.hasNext() ? it.next().get("value") : null;
    }

    /**
     * Returns {@code true} when a relationship of the given type exists between
     * two nodes identified by label and name.
     *
     * @param startLabel label of the relationship start node
     * @param startName  name of the start node
     * @param relType    relationship type, e.g. {@code "CONTAINS"} or {@code "PARENT_OF"}
     * @param endLabel   label of the relationship end node
     * @param endName    name of the end node
     */
    protected boolean relationshipExistsInGraph(final String startLabel,
                                                 final String startName,
                                                 final String relType,
                                                 final String endLabel,
                                                 final String endName) {
        String cypher = "MATCH (s:" + startLabel + " {name: $sName})-[:" + relType + "]->(e:" + endLabel + " {name: $eName}) RETURN s LIMIT 1";
        return runCypher(cypher, Map.of("sName", startName, "eName", endName)).iterator().hasNext();
    }

    /**
     * Returns the total number of File and Directory nodes in the test partition
     * database – useful for asserting that a recursive delete left nothing behind.
     */
    protected long countAllFileSystemNodes() {
        String cypher = "MATCH (n) WHERE n:File OR n:Directory RETURN count(n) AS cnt";
        var it = runCypher(cypher).iterator();
        if (!it.hasNext()) return 0L;
        Object val = it.next().get("cnt");
        return val instanceof Long l ? l : ((Number) val).longValue();
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Opens a fresh OGM session against the test partition database.  A single
     * {@link SessionFactory} is reused across all verification calls within one
     * test but is closed in teardown so it doesn't hold connections across tests.
     */
    private Session openVerificationSession() {
        if (verificationSessionFactory == null) {
            verificationSessionFactory = new SessionFactory(
                new Configuration.Builder()
                    .uri(config.neo4jUri)
                    .credentials(config.neo4jUsername, config.neo4jPassword)
                    .database(TEST_PARTITION)
                    .useNativeTypes()
                    .build(),
                "dev.scottsosna.neo4jfs.database.node");
        }
        return verificationSessionFactory.openSession();
    }
}
