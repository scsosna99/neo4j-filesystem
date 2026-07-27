/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for POSIX permission <em>inheritance</em> in Neo4Jfs.
 *
 * <p>When a file (or directory) has no explicit permissions stored on its own
 * node, the system falls back to the permissions of its parent directory —
 * referred to as "inherited permissions".  These tests verify:
 * <ol>
 *   <li>A file created without explicit permissions has a {@code null} permissions
 *       property in Neo4J.</li>
 *   <li>The inherited permissions from the parent are actually enforced for
 *       access-control decisions.</li>
 *   <li>Setting explicit permissions on a file overrides the inherited value and
 *       is persisted to Neo4J.</li>
 *   <li>Multi-level inheritance: a file whose parent also has no explicit
 *       permissions eventually inherits from a grandparent.</li>
 * </ol>
 */
@DisplayName("POSIX Permission Inheritance")
class PermissionInheritanceTest extends AbstractNeo4jfsIntegrationTest {

    // -------------------------------------------------------------------------
    // Null-permissions node in Neo4J
    // -------------------------------------------------------------------------

    /**
     * A file created via {@link Files#createFile} without a subsequent call to
     * {@link Files#setPosixFilePermissions} must store a {@code null} permissions
     * property in its Neo4J node.
     */
    @Test
    @DisplayName("file created without explicit permissions stores null in Neo4J")
    void fileWithNoExplicitPermissionsHasNullInGraph() throws IOException {
        // World-traversable parent owned by alice.
        Path home      = fileSystem.getPath("/home");
        Path aliceHome = fileSystem.getPath("/home/alice");
        Files.createDirectory(home);
        createOwnedDirectory(
            aliceHome, "alice", "alice",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.OTHERS_READ,
                       PosixFilePermission.OTHERS_EXECUTE));

        // Alice creates a file but never calls setPosixFilePermissions.
        setSecurityContext("alice", "alice");
        Path noPermsFile = fileSystem.getPath("/home/alice/no-perms.txt");
        Files.createFile(noPermsFile);

        // The file node must exist in the graph.
        assertTrue(nodeExistsInGraph("File", "no-perms.txt"),
            "File node must exist in Neo4J");

        // Its permissions property must be null (inheritance marker).
        assertNull(getNodeProperty("File", "no-perms.txt", "permissions"),
            "File with no explicit permissions must have null permissions in Neo4J");
    }

    // -------------------------------------------------------------------------
    // Inherited permissions are enforced
    // -------------------------------------------------------------------------

    /**
     * A file with no explicit permissions inherits those of its parent directory.
     * When the parent grants OTHERS_READ and OTHERS_EXECUTE, an unrelated user can
     * see the file (it is not hidden from them) and read its attributes.
     *
     * <p>This test uses {@link java.nio.file.Files#exists} and
     * {@link java.nio.file.Files#readAttributes} as the read-only probes.
     * The file's own Neo4J node must still have a {@code null} permissions property,
     * confirming that the inheritance mechanism — not an explicit entry — is what
     * grants access.
     */
    @Test
    @DisplayName("inherited OTHERS_READ from parent allows unrelated user to see the file")
    void inheritedPermissionsAllowReadAccess() throws IOException {
        // Alice's home: owner rwx, no group bits, others r-x.
        Path home      = fileSystem.getPath("/home");
        Path aliceHome = fileSystem.getPath("/home/alice");
        Files.createDirectory(home);
        createOwnedDirectory(
            aliceHome, "alice", "alice",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.OTHERS_READ,
                       PosixFilePermission.OTHERS_EXECUTE));

        // Alice creates a file without explicit permissions → inherits parent's "rwx---r-x".
        setSecurityContext("alice", "alice");
        Path inheritedFile = fileSystem.getPath("/home/alice/inherited.txt");
        Files.createFile(inheritedFile);

        // Confirm null permissions in graph.
        assertNull(getNodeProperty("File", "inherited.txt", "permissions"),
            "File must have null permissions in Neo4J — access is via inheritance only");

        // Bob (neither owner nor group member) uses the OTHERS bits inherited from the
        // parent directory.  OTHERS_READ + OTHERS_EXECUTE means the file is visible.
        setSecurityContext("bob", "bob");

        // Files.exists() must return true: Bob can see the file.
        assertTrue(Files.exists(fileSystem.getPath("/home/alice/inherited.txt")),
            "Inherited OTHERS_READ must make the file visible to bob via Files.exists");

        // Reading attributes must also succeed (read-only operation).
        assertDoesNotThrow(
            () -> Files.readAttributes(
                fileSystem.getPath("/home/alice/inherited.txt"),
                BasicFileAttributes.class),
            "Inherited OTHERS_READ must allow bob to read file attributes");
    }

    /**
     * When a parent directory restricts OTHERS access entirely (no read, write, or
     * execute for others), a file within it inherits those restrictive permissions and
     * is completely invisible to unrelated users.
     */
    @Test
    @DisplayName("inherited restrictive parent permissions deny access to others")
    void inheritedRestrictivePermissionsDenyAccess() throws IOException {
        // Admin sets up alice's home with no OTHERS access at all.
        Path home      = fileSystem.getPath("/home");
        Path aliceHome = fileSystem.getPath("/home/alice");
        Files.createDirectory(home);
        createOwnedDirectory(
            aliceHome, "alice", "alice",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE));

        // Alice creates a file – no explicit permissions on it.
        setSecurityContext("alice", "alice");
        Path hiddenFile = fileSystem.getPath("/home/alice/hidden.txt");
        Files.createFile(hiddenFile);

        // The file is there in the graph.
        assertTrue(nodeExistsInGraph("File", "hidden.txt"));
        assertNull(getNodeProperty("File", "hidden.txt", "permissions"),
            "Must have null permissions (inherits from parent)");

        // Bob cannot even locate the file: parent has no OTHERS_EXECUTE.
        // find() returns empty → NoSuchFileException from prologue().
        setSecurityContext("bob", "bob");
        assertThrows(AccessDeniedException.class,
            () -> Files.delete(fileSystem.getPath("/home/alice/hidden.txt")),
            "Bob must not be able to reach the file through the locked-down parent directory");

        // Alice can still delete her own file.
        setSecurityContext("alice", "alice");
        assertDoesNotThrow(() -> Files.delete(fileSystem.getPath("/home/alice/hidden.txt")),
            "Alice (owner) must be able to delete her own file regardless");

        assertFalse(nodeExistsInGraph("File", "hidden.txt"),
            "File node must be removed after deletion by owner");
    }

    // -------------------------------------------------------------------------
    // Explicit permissions override inheritance
    // -------------------------------------------------------------------------

    /**
     * Once explicit permissions are set on a file the inherited value is no longer
     * used.  The test confirms that:
     * <ul>
     *   <li>After {@link Files#setPosixFilePermissions} the Neo4J node carries a
     *       non-null permissions string.</li>
     *   <li>The explicit permissions govern access (not the parent's permissions).</li>
     * </ul>
     */
    @Test
    @DisplayName("explicit permissions override inherited permissions and are stored in Neo4J")
    void explicitPermissionsOverrideInheritance() throws IOException {
        // Parent directory: alice owns it, OTHERS get read+execute (inherited would allow others).
        Path home      = fileSystem.getPath("/home");
        Path aliceHome = fileSystem.getPath("/home/alice");
        Files.createDirectory(home);
        createOwnedDirectory(
            aliceHome, "alice", "alice",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.OTHERS_READ,
                       PosixFilePermission.OTHERS_EXECUTE));

        // Alice creates a file without explicit permissions → inherits parent's others-read.
        setSecurityContext("alice", "alice");
        Path overriddenFile = fileSystem.getPath("/home/alice/overridden.txt");
        Files.createFile(overriddenFile);

        // Initially: null permissions in graph (uses inheritance).
        assertNull(getNodeProperty("File", "overridden.txt", "permissions"),
            "Before explicit set, permissions must be null in Neo4J");

        // Alice now locks the file to owner-only.
        Files.setPosixFilePermissions(overriddenFile,
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

        // Neo4J must now carry the explicit permission string.
        assertEquals("rw-------", getNodeProperty("File", "overridden.txt", "permissions"),
            "Explicit permissions must be persisted to Neo4J, overriding null");

        // Bob previously could have read the file via inheritance; now he cannot
        // because the explicit permissions have no OTHERS bits.
        setSecurityContext("bob", "bob");
        assertThrows(AccessDeniedException.class,
            () -> Files.delete(fileSystem.getPath("/home/alice/overridden.txt")),
            "After explicit lock-down, bob must be denied even though parent allows others");

        // File must still be there.
        assertTrue(nodeExistsInGraph("File", "overridden.txt"),
            "File must remain in graph after failed access by bob");
    }

    // -------------------------------------------------------------------------
    // Multi-level inheritance
    // -------------------------------------------------------------------------

    /**
     * A subdirectory with no explicit permissions itself inherits from its parent.
     * A file inside that subdirectory therefore "chains" through two levels.
     * The test verifies that the outermost ancestor's permissions govern access for
     * an unrelated user.
     */
    @Test
    @DisplayName("multi-level inheritance: file inherits from grandparent when both intermediate nodes lack explicit permissions")
    void multiLevelInheritance() throws IOException {
        // Admin creates /data with group-only access (no OTHERS bits).
        Path data = fileSystem.getPath("/data");
        createOwnedDirectory(
            data, "root", "team",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.GROUP_READ,
                       PosixFilePermission.GROUP_WRITE,
                       PosixFilePermission.GROUP_EXECUTE));

        // Admin creates /data/sub as a team member – no explicit permissions set on it.
        setSecurityContext("root", "team");
        Path sub = fileSystem.getPath("/data/sub");
        Files.createDirectory(sub);
        // sub inherits from /data: null permissions, team-r/w/x for group.

        // A team member creates a file in /data/sub without explicit permissions.
        setSecurityContext("alice", "team");
        Path deepFile = fileSystem.getPath("/data/sub/deep.txt");
        Files.createFile(deepFile);

        // Graph: both sub and deep.txt must have null permissions.
        assertNull(getNodeProperty("Directory", "sub", "permissions"),
            "Subdirectory created without explicit permissions must have null in Neo4J");
        assertNull(getNodeProperty("File", "deep.txt", "permissions"),
            "File created without explicit permissions must have null in Neo4J");

        // Carol is in team → she can traverse /data (GROUP_EXECUTE), /data/sub (inherited
        // GROUP_EXECUTE), and read deep.txt (inherited GROUP_READ).
        setSecurityContext("carol", "team");
        assertDoesNotThrow(() -> Files.delete(fileSystem.getPath("/data/sub/deep.txt")),
            "Team member must reach and delete the file through inherited group permissions");

        assertFalse(nodeExistsInGraph("File", "deep.txt"),
            "File node must be gone after deletion by team member");

        // Recreate for the second assertion.
        setSecurityContext("alice", "team");
        Files.createFile(fileSystem.getPath("/data/sub/deep.txt"));

        // Bob is NOT in team → he has no access (no OTHERS bits at any level).
        setSecurityContext("bob", "bob");
        assertThrows(AccessDeniedException.class,
            () -> Files.delete(fileSystem.getPath("/data/sub/deep.txt")),
            "Non-team user must be denied access through inherited group-only permissions");

        assertTrue(nodeExistsInGraph("File", "deep.txt"),
            "File must remain in graph after failed deletion by non-team user");
    }
}
