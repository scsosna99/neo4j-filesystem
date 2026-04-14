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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for POSIX permission enforcement in Neo4Jfs.
 *
 * <p>Each test builds a minimal directory/file structure, performs operations
 * under different users, and then confirms the graph state in Neo4J via Cypher
 * queries.  The storage manager is configured as "dummy" so no real files are
 * written to disk.
 *
 * <p>Permission-check semantics:
 * <ul>
 *   <li>The admin user ({@code root}/{@code wheel}) always has full access.</li>
 *   <li>Accessing a path checks READ on the target and EXECUTE on its parent.</li>
 *   <li>When access is denied the implementation throws {@link NoSuchFileException}
 *       (hiding the entry from the caller) rather than {@link AccessDeniedException}.</li>
 *   <li>Writing to a directory (creating a child) requires WRITE + EXECUTE on the
 *       parent directory.</li>
 * </ul>
 */
@DisplayName("POSIX Permission Enforcement")
class PermissionEnforcementTest extends AbstractNeo4jfsIntegrationTest {

    // -------------------------------------------------------------------------
    // Admin access
    // -------------------------------------------------------------------------

    /**
     * The admin (root) user must always succeed regardless of the permissions
     * stored on a file.  This verifies the "admin bypass" path in
     * {@link dev.scottsosna.neo4jfs.security.PosixAccessManager}.
     */
    @Test
    @DisplayName("admin can delete any file regardless of permissions")
    void adminCanDeleteAnyFile() throws IOException {
        // Admin builds a directory owned by alice with owner-only permissions.
        Path home    = fileSystem.getPath("/home");
        Path aliceHome = fileSystem.getPath("/home/alice");
        Files.createDirectory(home);
        createOwnedDirectory(
            aliceHome, "alice", "alice",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE));

        // Alice creates a private file (owner-only).
        setSecurityContext("alice", "alice");
        Path privateFile = fileSystem.getPath("/home/alice/private.txt");
        createFileWithPermissions(
            privateFile,
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

        // Confirm the file is in the graph.
        assertTrue(nodeExistsInGraph("File", "private.txt"),
            "File node must exist in Neo4J before deletion");
        assertTrue(relationshipExistsInGraph("Directory", "alice", "CONTAINS", "File", "private.txt"),
            "alice directory must CONTAIN the file");

        // Admin deletes the file – should succeed unconditionally.
        setSecurityContext(accessManager.getAdminUser(), accessManager.getAdminGroup());
        assertDoesNotThrow(() -> Files.delete(privateFile),
            "Admin must be able to delete any file");

        // Confirm the file node is gone from the graph.
        assertFalse(nodeExistsInGraph("File", "private.txt"),
            "File node must be removed from Neo4J after deletion");
        assertFalse(relationshipExistsInGraph("Directory", "alice", "CONTAINS", "File", "private.txt"),
            "CONTAINS relationship must be removed after deletion");
    }

    // -------------------------------------------------------------------------
    // Owner access
    // -------------------------------------------------------------------------

    /**
     * The file owner can delete their own file when the owner-write bit is set.
     */
    @Test
    @DisplayName("owner can delete own file")
    void ownerCanDeleteOwnFile() throws IOException {
        // Admin creates a world-writable workspace directory (rwxrwxrwx so any user can create files).
        Path workspace = fileSystem.getPath("/workspace");
        createOwnedDirectory(
            workspace, "root", "wheel",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.GROUP_READ,
                       PosixFilePermission.GROUP_WRITE,
                       PosixFilePermission.GROUP_EXECUTE,
                       PosixFilePermission.OTHERS_READ,
                       PosixFilePermission.OTHERS_WRITE,
                       PosixFilePermission.OTHERS_EXECUTE));

        // Alice creates her own file.
        setSecurityContext("alice", "alice");
        Path aliceFile = fileSystem.getPath("/workspace/alice-data.txt");
        Files.createFile(aliceFile);

        // Verify graph: file exists and is owned by alice.
        assertTrue(nodeExistsInGraph("File", "alice-data.txt"));
        assertEquals("alice", getNodeProperty("File", "alice-data.txt", "ownerUserName"),
            "File must be owned by alice");

        // Alice deletes her own file – should succeed.
        assertDoesNotThrow(() -> Files.delete(aliceFile), "Owner must be able to delete own file");

        // Confirm file node removed from graph.
        assertFalse(nodeExistsInGraph("File", "alice-data.txt"),
            "File node must be gone after deletion by owner");
    }

    // -------------------------------------------------------------------------
    // Restricted-directory traversal
    // -------------------------------------------------------------------------

    /**
     * When a directory's execute bit is not set for others, a non-owner cannot
     * traverse into it.  The implementation returns {@link NoSuchFileException}
     * (hiding the entry) rather than {@link AccessDeniedException}.
     */
    @Test
    @DisplayName("non-owner cannot traverse directory without execute permission")
    void nonOwnerCannotTraverseRestrictedDirectory() throws IOException {
        // Admin creates alice's home directory with no OTHERS_EXECUTE.
        Path home      = fileSystem.getPath("/home");
        Path aliceHome = fileSystem.getPath("/home/alice");
        Files.createDirectory(home);
        createOwnedDirectory(
            aliceHome, "alice", "alice",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE));
        // No group or others access → bob cannot enter.

        // Alice creates a file inside her private directory.
        setSecurityContext("alice", "alice");
        Path aliceFile = fileSystem.getPath("/home/alice/secret.txt");
        Files.createFile(aliceFile);

        // Verify the file exists in the graph.
        assertTrue(nodeExistsInGraph("File", "secret.txt"));

        // Bob attempts to delete alice's file – must fail because he cannot traverse /home/alice.
        setSecurityContext("bob", "bob");
        assertThrows(NoSuchFileException.class,
            () -> Files.delete(fileSystem.getPath("/home/alice/secret.txt")),
            "Bob must be denied traversal into alice's owner-only directory");

        // The file must still exist in the graph – it was not deleted.
        assertTrue(nodeExistsInGraph("File", "secret.txt"),
            "File must still exist in graph after failed deletion attempt");
    }

    // -------------------------------------------------------------------------
    // File-level write permission
    // -------------------------------------------------------------------------

    /**
     * Even when the parent directory is world-traversable, a file with owner-only
     * write permission must block deletion by a different user.
     */
    @Test
    @DisplayName("non-owner is denied delete on owner-only file in accessible directory")
    void nonOwnerCannotDeleteOwnerOnlyFile() throws IOException {
        // World-traversable directory owned by alice.
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

        // Alice creates a file and restricts it to owner-only.
        setSecurityContext("alice", "alice");
        Path privateFile = fileSystem.getPath("/home/alice/private.txt");
        createFileWithPermissions(
            privateFile,
            EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));

        // Confirm permissions are persisted correctly in the graph.
        assertEquals("rw-------", getNodeProperty("File", "private.txt", "permissions"),
            "Neo4J must store owner-only permission string");

        // Bob can read the directory (OTHERS_READ+EXECUTE on parent) but cannot delete
        // the file because it has no OTHERS_WRITE.  Access is denied at the file level
        // because Bob cannot read the file (no OTHERS_READ) → NoSuchFileException.
        setSecurityContext("bob", "bob");
        assertThrows(NoSuchFileException.class,
            () -> Files.delete(fileSystem.getPath("/home/alice/private.txt")),
            "Bob must be denied deletion of owner-only file");

        // File must still exist.
        assertTrue(nodeExistsInGraph("File", "private.txt"),
            "File must still exist in graph after failed deletion by non-owner");
    }

    // -------------------------------------------------------------------------
    // World-writable directory
    // -------------------------------------------------------------------------

    /**
     * Any user can create a file in a directory that has the OTHERS_WRITE and
     * OTHERS_EXECUTE bits set.  Both the NIO operation and the graph state are verified.
     */
    @Test
    @DisplayName("any user can create a file in a world-writable directory")
    void anyUserCanCreateFileInWorldWritableDirectory() throws IOException {
        // Admin creates a world-writable shared directory.
        Path shared = fileSystem.getPath("/shared");
        createOwnedDirectory(
            shared, "root", "wheel",
            EnumSet.allOf(PosixFilePermission.class));   // rwxrwxrwx

        // Bob creates a file in the shared directory.
        setSecurityContext("bob", "bob");
        Path bobFile = fileSystem.getPath("/shared/bob-upload.txt");
        assertDoesNotThrow(() -> Files.createFile(bobFile),
            "Bob must be able to create a file in a world-writable directory");

        // Verify graph: file exists and bob is the owner.
        assertTrue(nodeExistsInGraph("File", "bob-upload.txt"),
            "File node must exist in Neo4J");
        assertEquals("bob", getNodeProperty("File", "bob-upload.txt", "ownerUserName"),
            "File must be owned by the creating user (bob)");
        assertTrue(relationshipExistsInGraph("Directory", "shared", "CONTAINS", "File", "bob-upload.txt"),
            "shared directory must CONTAIN the new file");
    }

    // -------------------------------------------------------------------------
    // Group-based access
    // -------------------------------------------------------------------------

    /**
     * A user that presents the owning group as an authority gains group-level access
     * to a file restricted to group members.
     */
    @Test
    @DisplayName("group member can delete a group-write file")
    void groupMemberCanDeleteGroupWriteFile() throws IOException {
        // Admin creates a directory owned by the "project" group.
        Path project = fileSystem.getPath("/project");
        createOwnedDirectory(
            project, "alice", "project",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.GROUP_READ,
                       PosixFilePermission.GROUP_WRITE,
                       PosixFilePermission.GROUP_EXECUTE));

        // Alice creates a file with group-write permissions.
        setSecurityContext("alice", "project");
        Path groupFile = fileSystem.getPath("/project/shared-doc.txt");
        createFileWithPermissions(
            groupFile,
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.GROUP_READ,
                       PosixFilePermission.GROUP_WRITE));

        // Verify permissions stored in graph.
        assertEquals("rw-rw----", getNodeProperty("File", "shared-doc.txt", "permissions"),
            "Group-writable permission string must be persisted in Neo4J");

        // Bob is a member of the "project" group → he can delete the file.
        setSecurityContext("bob", "project");
        assertDoesNotThrow(() -> Files.delete(fileSystem.getPath("/project/shared-doc.txt")),
            "Group member must be able to delete group-writable file");

        assertFalse(nodeExistsInGraph("File", "shared-doc.txt"),
            "File node must be removed from Neo4J after deletion by group member");
    }

    /**
     * A user that does NOT present the owning group as an authority is blocked from
     * accessing a group-restricted file.
     */
    @Test
    @DisplayName("non-group-member cannot delete group-only file")
    void nonGroupMemberCannotDeleteGroupOnlyFile() throws IOException {
        // Admin creates a group-only directory.
        Path project = fileSystem.getPath("/project");
        createOwnedDirectory(
            project, "alice", "project",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.GROUP_READ,
                       PosixFilePermission.GROUP_WRITE,
                       PosixFilePermission.GROUP_EXECUTE));

        // Alice creates a group-only file.
        setSecurityContext("alice", "project");
        Path groupFile = fileSystem.getPath("/project/restricted.txt");
        createFileWithPermissions(
            groupFile,
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.GROUP_READ,
                       PosixFilePermission.GROUP_WRITE));

        // Carol is NOT in the "project" group → she cannot access.
        setSecurityContext("carol", "carol");
        assertThrows(NoSuchFileException.class,
            () -> Files.delete(fileSystem.getPath("/project/restricted.txt")),
            "Non-group-member must be denied deletion of group-only file");

        // File must remain.
        assertTrue(nodeExistsInGraph("File", "restricted.txt"),
            "File must still exist in graph after failed deletion by non-group-member");

        // If carol is granted the "project" authority the same path must now succeed.
        setSecurityContext("carol", "project");
        assertDoesNotThrow(
            () -> Files.delete(fileSystem.getPath("/project/restricted.txt")),
            "Carol succeeds after being granted project group membership");

        assertFalse(nodeExistsInGraph("File", "restricted.txt"),
            "File node must be gone after group-authorized deletion");
    }

    // -------------------------------------------------------------------------
    // Ownership and permission persistence in Neo4J
    // -------------------------------------------------------------------------

    /**
     * Verifies that the Neo4J graph records the correct owner, group, and
     * permission string after a directory is created and its attributes set.
     */
    @Test
    @DisplayName("directory ownership and permissions are persisted correctly in Neo4J")
    void directoryAttributesPersistedInNeo4j() throws IOException {
        Path home      = fileSystem.getPath("/home");
        Path aliceHome = fileSystem.getPath("/home/alice");
        Files.createDirectory(home);
        createOwnedDirectory(
            aliceHome, "alice", "alice",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE,
                       PosixFilePermission.GROUP_READ,
                       PosixFilePermission.OTHERS_READ));

        // Graph assertions.
        assertTrue(nodeExistsInGraph("Directory", "alice"),
            "Directory node must exist in Neo4J");
        assertEquals("alice", getNodeProperty("Directory", "alice", "ownerUserName"),
            "Directory owner must be alice");
        assertEquals("alice", getNodeProperty("Directory", "alice", "ownerGroupName"),
            "Directory group must be alice");
        assertEquals("rwxr--r--", getNodeProperty("Directory", "alice", "permissions"),
            "Permission string must be stored as rwxr--r--");
        assertTrue(relationshipExistsInGraph("Directory", "home", "PARENT_OF", "Directory", "alice"),
            "home directory must have PARENT_OF relationship to alice");
    }

    /**
     * Verifies that calling {@link Files#setPosixFilePermissions} updates the
     * permissions property stored in Neo4J.
     */
    @Test
    @DisplayName("changing file permissions is reflected in Neo4J")
    void permissionChangeReflectedInNeo4j() throws IOException {
        // World-writable workspace (rwxrwxrwx so alice can create and change files there).
        Path workspace = fileSystem.getPath("/workspace");
        createOwnedDirectory(
            workspace, "root", "wheel",
            EnumSet.allOf(PosixFilePermission.class));

        // Alice creates a file initially with full owner+group permissions.
        setSecurityContext("alice", "alice");
        Path aliceFile = fileSystem.getPath("/workspace/changeable.txt");
        createFileWithPermissions(
            aliceFile,
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.GROUP_READ,
                       PosixFilePermission.GROUP_WRITE));

        assertEquals("rw-rw----", getNodeProperty("File", "changeable.txt", "permissions"),
            "Initial permissions must be stored correctly");

        // Alice locks the file down to owner-read-only.
        Files.setPosixFilePermissions(aliceFile, EnumSet.of(PosixFilePermission.OWNER_READ));

        assertEquals("r--------", getNodeProperty("File", "changeable.txt", "permissions"),
            "Updated permissions must be reflected in Neo4J immediately");
    }

    // -------------------------------------------------------------------------
    // Copy and move operations under permission constraints
    // -------------------------------------------------------------------------

    /**
     * Copying a file into a world-writable directory must succeed, and the new
     * copy must appear as an independent node in the graph owned by the copying user.
     */
    @Test
    @DisplayName("any user can copy file into world-writable directory")
    void anyUserCanCopyFileIntoWorldWritableDirectory() throws IOException {
        // Build a small source structure (owned by alice).
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

        // Alice uploads a shared file.
        setSecurityContext("alice", "alice");
        Path sharedFile = fileSystem.getPath("/home/alice/shared.txt");
        createFileWithPermissions(
            sharedFile,
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OTHERS_READ));

        // World-writable target directory.
        setSecurityContext(accessManager.getAdminUser(), accessManager.getAdminGroup());
        Path target = fileSystem.getPath("/target");
        createOwnedDirectory(target, "root", "wheel", EnumSet.allOf(PosixFilePermission.class));

        // Bob copies alice's shared file into the target under a distinct name.
        setSecurityContext("bob", "bob");
        assertDoesNotThrow(
            () -> Files.copy(fileSystem.getPath("/home/alice/shared.txt"),
                             fileSystem.getPath("/target/bob-copy.txt")),
            "Bob must be able to copy a world-readable file into a world-writable directory");

        // Verify: the original still exists and the copy appears as a new node.
        assertTrue(nodeExistsInGraph("File", "shared.txt"),
            "Source file must still exist in the graph after copy");
        assertTrue(nodeExistsInGraph("File", "bob-copy.txt"),
            "Copied file must appear in the graph");
        assertTrue(relationshipExistsInGraph("Directory", "target", "CONTAINS", "File", "bob-copy.txt"),
            "target directory must CONTAIN the copied file");
    }

    /**
     * Moving a file between directories within the same file system must remove the
     * original CONTAINS relationship and add a new one at the destination.
     */
    @Test
    @DisplayName("moving a file updates relationships in Neo4J")
    void movingFileUpdatesGraphRelationships() throws IOException {
        // Admin sets up source and destination directories (world-accessible).
        Path source = fileSystem.getPath("/source");
        Path dest   = fileSystem.getPath("/dest");
        createOwnedDirectory(source, "root", "wheel", EnumSet.allOf(PosixFilePermission.class));
        createOwnedDirectory(dest,   "root", "wheel", EnumSet.allOf(PosixFilePermission.class));

        // Alice creates a file in /source.
        setSecurityContext("alice", "alice");
        Path original = fileSystem.getPath("/source/moveme.txt");
        Files.createFile(original);

        assertTrue(nodeExistsInGraph("File", "moveme.txt"));
        assertTrue(relationshipExistsInGraph("Directory", "source", "CONTAINS", "File", "moveme.txt"),
            "Source directory must CONTAIN file before move");

        // Alice moves the file to /dest (world-writable → she can write there).
        assertDoesNotThrow(
            () -> Files.move(fileSystem.getPath("/source/moveme.txt"),
                             fileSystem.getPath("/dest/moveme.txt")),
            "Owner must be able to move own file to world-writable destination");

        // After the move: /dest CONTAINS the file, /source does not.
        assertTrue(relationshipExistsInGraph("Directory", "dest", "CONTAINS", "File", "moveme.txt"),
            "dest directory must CONTAIN file after move");
        assertFalse(relationshipExistsInGraph("Directory", "source", "CONTAINS", "File", "moveme.txt"),
            "source directory must no longer CONTAIN file after move");
    }
}
