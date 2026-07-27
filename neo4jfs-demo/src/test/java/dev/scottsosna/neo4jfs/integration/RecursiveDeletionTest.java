/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.integration;

import dev.scottsosna.neo4jfs.filesystem.option.Neo4jfsDeleteOption;
import dev.scottsosna.neo4jfs.service.DirectoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for directory deletion in Neo4Jfs, covering both the
 * standard (non-recursive) delete and the recursive
 * {@link Neo4jfsDeleteOption#DELETE_RECURSIVELY} extension.
 *
 * <p>Each test verifies behaviour via the NIO.2 API and then interrogates Neo4J
 * directly to confirm that the correct nodes and relationships were (or were not)
 * removed from the graph.
 */
@DisplayName("Recursive and Standard Directory Deletion")
class RecursiveDeletionTest extends AbstractNeo4jfsIntegrationTest {

    /** DirectoryService used for the recursive-delete overload. */
    @Autowired
    private DirectoryService directoryService;

    // -------------------------------------------------------------------------
    // Standard (non-recursive) delete
    // -------------------------------------------------------------------------

    /**
     * Deleting a non-empty directory via the standard NIO.2 API must throw
     * {@link DirectoryNotEmptyException}; the directory and its contents must
     * remain intact in the graph.
     */
    @Test
    @DisplayName("standard delete of non-empty directory throws DirectoryNotEmptyException")
    void standardDeleteOfNonEmptyDirectoryFails() throws IOException {
        // Build: /tree/leaves/leaf.txt
        Path tree   = fileSystem.getPath("/tree");
        Path leaves = fileSystem.getPath("/tree/leaves");
        createOwnedDirectory(tree,   "root", "wheel", EnumSet.allOf(PosixFilePermission.class));
        createOwnedDirectory(leaves, "root", "wheel", EnumSet.allOf(PosixFilePermission.class));

        setSecurityContext(accessManager.getAdminUser(), accessManager.getAdminGroup());
        Files.createFile(fileSystem.getPath("/tree/leaves/leaf.txt"));

        // Verify graph before any deletion.
        assertTrue(nodeExistsInGraph("Directory", "tree"),   "tree must exist");
        assertTrue(nodeExistsInGraph("Directory", "leaves"), "leaves must exist");
        assertTrue(nodeExistsInGraph("File",      "leaf.txt"), "leaf.txt must exist");

        // Standard delete of the non-empty /tree/leaves must fail.
        assertThrows(DirectoryNotEmptyException.class,
            () -> Files.delete(fileSystem.getPath("/tree/leaves")),
            "Deleting a non-empty directory without the recursive option must throw DirectoryNotEmptyException");

        // The graph must be untouched.
        assertTrue(nodeExistsInGraph("Directory", "leaves"), "leaves must still exist after failed delete");
        assertTrue(nodeExistsInGraph("File",      "leaf.txt"), "leaf.txt must still exist after failed delete");
    }

    /**
     * An empty directory can be removed with the standard NIO.2 delete;
     * the node and the parent's PARENT_OF relationship must disappear.
     */
    @Test
    @DisplayName("standard delete of empty directory succeeds and removes node from Neo4J")
    void standardDeleteOfEmptyDirectorySucceeds() throws IOException {
        Path parent = fileSystem.getPath("/parent");
        Path empty  = fileSystem.getPath("/parent/empty");
        createOwnedDirectory(parent, "root", "wheel", EnumSet.allOf(PosixFilePermission.class));
        createOwnedDirectory(empty,  "root", "wheel", EnumSet.allOf(PosixFilePermission.class));

        assertTrue(nodeExistsInGraph("Directory", "empty"));
        assertTrue(relationshipExistsInGraph("Directory", "parent", "PARENT_OF", "Directory", "empty"));

        // Delete the empty directory.
        assertDoesNotThrow(() -> Files.delete(fileSystem.getPath("/parent/empty")),
            "Empty directory must be deleted without error");

        // Node and relationship must be gone.
        assertFalse(nodeExistsInGraph("Directory", "empty"),
            "empty directory node must be removed from Neo4J");
        assertFalse(relationshipExistsInGraph("Directory", "parent", "PARENT_OF", "Directory", "empty"),
            "PARENT_OF relationship must be removed from Neo4J");
    }

    // -------------------------------------------------------------------------
    // Recursive delete via DirectoryService
    // -------------------------------------------------------------------------

    /**
     * {@link DirectoryService#delete(java.net.URI, Neo4jfsDeleteOption...)} with
     * {@link Neo4jfsDeleteOption#DELETE_RECURSIVELY} must remove the target directory
     * together with all descendant directories and files.  The graph must contain no
     * File or Directory nodes for the deleted subtree after the operation.
     */
    @Test
    @DisplayName("recursive delete removes directory tree and all graph nodes")
    void recursiveDeleteRemovesEntireSubtree() throws IOException {
        // Build a small tree: /project → /project/src → /project/src/Main.java
        //                                              → /project/src/Util.java
        //                    /project/docs → /project/docs/readme.txt
        setSecurityContext(accessManager.getAdminUser(), accessManager.getAdminGroup());

        Path project     = fileSystem.getPath("/project");
        Path src         = fileSystem.getPath("/project/src");
        Path docs        = fileSystem.getPath("/project/docs");
        createOwnedDirectory(project, "root", "wheel", EnumSet.allOf(PosixFilePermission.class));
        createOwnedDirectory(src,     "root", "wheel", EnumSet.allOf(PosixFilePermission.class));
        createOwnedDirectory(docs,    "root", "wheel", EnumSet.allOf(PosixFilePermission.class));

        Files.createFile(fileSystem.getPath("/project/src/Main.java"));
        Files.createFile(fileSystem.getPath("/project/src/Util.java"));
        Files.createFile(fileSystem.getPath("/project/docs/readme.txt"));

        // Verify the full tree is in the graph before deletion.
        assertTrue(nodeExistsInGraph("Directory", "project"), "project must exist");
        assertTrue(nodeExistsInGraph("Directory", "src"),     "src must exist");
        assertTrue(nodeExistsInGraph("Directory", "docs"),    "docs must exist");
        assertTrue(nodeExistsInGraph("File", "Main.java"),    "Main.java must exist");
        assertTrue(nodeExistsInGraph("File", "Util.java"),    "Util.java must exist");
        assertTrue(nodeExistsInGraph("File", "readme.txt"),   "readme.txt must exist");

        // Recursively delete /project.
        assertDoesNotThrow(
            () -> directoryService.delete(
                fileSystem.getPath("/project").toUri(),
                Neo4jfsDeleteOption.DELETE_RECURSIVELY),
            "Recursive delete must complete without error");

        // Every node in the deleted subtree must be gone.
        assertFalse(nodeExistsInGraph("Directory", "project"), "project must be removed");
        assertFalse(nodeExistsInGraph("Directory", "src"),     "src must be removed");
        assertFalse(nodeExistsInGraph("Directory", "docs"),    "docs must be removed");
        assertFalse(nodeExistsInGraph("File", "Main.java"),    "Main.java must be removed");
        assertFalse(nodeExistsInGraph("File", "Util.java"),    "Util.java must be removed");
        assertFalse(nodeExistsInGraph("File", "readme.txt"),   "readme.txt must be removed");

        // The root node must still exist – only the deleted subtree is gone.
        assertTrue(nodeExistsInGraph("Directory", "/"),
            "Root directory must survive the recursive delete");
    }

    /**
     * After a recursive delete the entire test-partition graph (excluding the root
     * created by file-system initialisation) must be empty.
     */
    @Test
    @DisplayName("after recursive delete no File or Directory nodes remain in graph")
    void graphIsEmptyAfterRecursiveDelete() throws IOException {
        setSecurityContext(accessManager.getAdminUser(), accessManager.getAdminGroup());

        Path data = fileSystem.getPath("/data");
        createOwnedDirectory(data, "root", "wheel", EnumSet.allOf(PosixFilePermission.class));
        Files.createFile(fileSystem.getPath("/data/a.txt"));
        Files.createFile(fileSystem.getPath("/data/b.txt"));

        // Count before: root + /data + 2 files = 4 nodes (root is always present).
        long before = countAllFileSystemNodes();
        assertTrue(before >= 4L,
            "Graph must have at least 4 nodes before deletion (root + data + 2 files)");

        // Recursively delete /data.
        directoryService.delete(
            fileSystem.getPath("/data").toUri(),
            Neo4jfsDeleteOption.DELETE_RECURSIVELY);

        // After deletion only the root directory should remain.
        long after = countAllFileSystemNodes();
        assertEquals(1L, after,
            "Only the root Directory node must remain after recursive delete of /data");
    }

    // -------------------------------------------------------------------------
    // Permission check on recursive delete
    // -------------------------------------------------------------------------

    /**
     * An unprivileged user cannot recursively delete a directory they do not own
     * (or do not have write access to).  The graph must be unchanged after the
     * failed attempt.
     */
    @Test
    @DisplayName("non-owner cannot recursively delete a directory without write permission")
    void nonOwnerCannotRecursivelyDeleteLockedDirectory() throws IOException {
        // Admin creates a directory owned and locked down to alice.
        Path vault = fileSystem.getPath("/vault");
        createOwnedDirectory(
            vault, "alice", "alice",
            EnumSet.of(PosixFilePermission.OWNER_READ,
                       PosixFilePermission.OWNER_WRITE,
                       PosixFilePermission.OWNER_EXECUTE));

        setSecurityContext("alice", "alice");
        Files.createFile(fileSystem.getPath("/vault/treasure.txt"));

        // Bob cannot traverse /vault (no OTHERS_EXECUTE) → NoSuchFileException.
        setSecurityContext("bob", "bob");
        assertThrows(AccessDeniedException.class,
            () -> directoryService.delete(
                fileSystem.getPath("/vault").toUri(),
                Neo4jfsDeleteOption.DELETE_RECURSIVELY),
            "Bob must not be able to recursively delete alice's locked vault");

        // Both nodes must still be in the graph.
        assertTrue(nodeExistsInGraph("Directory", "vault"),
            "vault directory must remain in graph");
        assertTrue(nodeExistsInGraph("File", "treasure.txt"),
            "treasure.txt must remain in graph");
    }
}
