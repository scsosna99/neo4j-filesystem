/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.integration;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.EnumSet;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the read-only file system mode in Neo4Jfs.
 *
 * <p>A file system opened with {@code env.put("read-only", true)} must reject all
 * mutating NIO.2 operations with {@link ReadOnlyFileSystemException} while
 * continuing to serve read-only operations normally.
 *
 * <p>Test lifecycle:
 * <ol>
 *   <li>The parent {@code @BeforeEach} ({@link AbstractNeo4jfsIntegrationTest#setUpFileSystem()})
 *       opens a read-write file system and stores it in {@code fileSystem}.</li>
 *   <li>The child {@code @BeforeEach} ({@link #switchToReadOnlyMode()}) uses that
 *       window to pre-populate the partition with a known directory/file layout,
 *       then closes the read-write instance and reopens the same partition as
 *       read-only.</li>
 *   <li>Each test exercises the read-only file system.</li>
 *   <li>The parent {@code @AfterEach} closes the file system and drops the partition
 *       database, giving the next test a clean slate.</li>
 * </ol>
 */
@DisplayName("Read-Only File System Enforcement")
class ReadOnlyFileSystemTest extends AbstractNeo4jfsIntegrationTest {

    /** Path that exists before the file system is made read-only. */
    private static final String EXISTING_DIR  = "/data";
    private static final String EXISTING_FILE = "/data/pre-existing.txt";

    /**
     * Pre-populates the partition with known content while the file system is
     * still read-write, then closes it and reopens it in read-only mode.
     *
     * <p>JUnit 5 calls parent {@code @BeforeEach} first, then this method.
     */
    @BeforeEach
    void switchToReadOnlyMode() throws IOException {
        // The parent setUp already set admin context and opened a read-write FS.
        // Create a small known structure before sealing the file system.
        setSecurityContext(accessManager.getAdminUser(), accessManager.getAdminGroup());

        Path dataDir = fileSystem.getPath(EXISTING_DIR);
        createOwnedDirectory(dataDir, "root", "wheel", EnumSet.allOf(PosixFilePermission.class));
        Files.createFile(fileSystem.getPath(EXISTING_FILE));

        // Close the read-write instance (removes it from the provider's registry).
        fileSystem.close();

        // Reopen the same partition in read-only mode.
        fileSystem = FileSystems.newFileSystem(
            TEST_FS_URI,
            Map.of(Neo4jfsConstants.FILE_SYSTEM_ENV_READ_ONLY, true));

        assertTrue(fileSystem.isReadOnly(),
            "File system must report itself as read-only after being opened with read-only=true");
    }

    // -------------------------------------------------------------------------
    // Write operations must be blocked
    // -------------------------------------------------------------------------

    /**
     * Creating a new directory in a read-only file system must throw
     * {@link ReadOnlyFileSystemException}.  The graph must be unchanged.
     */
    @Test
    @DisplayName("createDirectory throws ReadOnlyFileSystemException")
    void createDirectoryThrowsOnReadOnly() {
        assertThrows(ReadOnlyFileSystemException.class,
            () -> Files.createDirectory(fileSystem.getPath("/newdir")),
            "createDirectory must throw ReadOnlyFileSystemException on a read-only file system");

        assertFalse(nodeExistsInGraph("Directory", "newdir"),
            "No new Directory node must appear in Neo4J after the failed create");
    }

    /**
     * Creating a new file must be rejected with {@link ReadOnlyFileSystemException}.
     */
    @Test
    @DisplayName("createFile throws ReadOnlyFileSystemException")
    void createFileThrowsOnReadOnly() {
        assertThrows(ReadOnlyFileSystemException.class,
            () -> Files.createFile(fileSystem.getPath("/data/new.txt")),
            "createFile must throw ReadOnlyFileSystemException on a read-only file system");

        assertFalse(nodeExistsInGraph("File", "new.txt"),
            "No new File node must appear in Neo4J after the failed create");
    }

    /**
     * Deleting an existing file must be rejected.
     */
    @Test
    @DisplayName("delete throws ReadOnlyFileSystemException")
    void deleteThrowsOnReadOnly() {
        assertThrows(ReadOnlyFileSystemException.class,
            () -> Files.delete(fileSystem.getPath(EXISTING_FILE)),
            "delete must throw ReadOnlyFileSystemException on a read-only file system");

        // The file must still be in the graph.
        assertTrue(nodeExistsInGraph("File", "pre-existing.txt"),
            "File must remain in Neo4J after failed delete on read-only file system");
    }

    /**
     * Deleting an existing directory must be rejected.
     */
    @Test
    @DisplayName("delete of directory throws ReadOnlyFileSystemException")
    void deleteDirectoryThrowsOnReadOnly() throws IOException {
        // First delete the file so the directory is empty and could otherwise be deleted.
        // We must switch to read-write temporarily to do this pre-condition setup.
        // Since we cannot modify the file system here we verify that even an empty dir
        // cannot be deleted; but /data still has the file so this check is sufficient.
        assertThrows(ReadOnlyFileSystemException.class,
            () -> Files.delete(fileSystem.getPath(EXISTING_DIR)),
            "delete of directory must throw ReadOnlyFileSystemException on a read-only file system");

        assertTrue(nodeExistsInGraph("Directory", "data"),
            "data directory must remain in Neo4J after failed delete");
    }

    /**
     * Setting POSIX permissions on an existing path must be rejected.
     */
    @Test
    @DisplayName("setPosixFilePermissions throws ReadOnlyFileSystemException")
    void setPermissionsThrowsOnReadOnly() {
        assertThrows(ReadOnlyFileSystemException.class,
            () -> Files.setPosixFilePermissions(
                fileSystem.getPath(EXISTING_FILE),
                EnumSet.of(PosixFilePermission.OWNER_READ)),
            "setPosixFilePermissions must throw ReadOnlyFileSystemException on a read-only file system");

        // Permissions in Neo4J must be unchanged (still null – the file was created
        // without explicit permissions and the set was blocked).
        assertNull(getNodeProperty("File", "pre-existing.txt", "permissions"),
            "Permissions must remain null in Neo4J after a failed setAttribute on read-only FS");
    }

    /**
     * Copying a file must be rejected because the target write is blocked.
     */
    @Test
    @DisplayName("copy throws ReadOnlyFileSystemException")
    void copyThrowsOnReadOnly() {
        assertThrows(ReadOnlyFileSystemException.class,
            () -> Files.copy(
                fileSystem.getPath(EXISTING_FILE),
                fileSystem.getPath("/data/copy.txt")),
            "copy must throw ReadOnlyFileSystemException on a read-only file system");

        assertFalse(nodeExistsInGraph("File", "copy.txt"),
            "No copy File node must appear in Neo4J after failed copy on read-only FS");
    }

    /**
     * Moving a file must be rejected.
     */
    @Test
    @DisplayName("move throws ReadOnlyFileSystemException")
    void moveThrowsOnReadOnly() {
        assertThrows(ReadOnlyFileSystemException.class,
            () -> Files.move(
                fileSystem.getPath(EXISTING_FILE),
                fileSystem.getPath("/data/moved.txt")),
            "move must throw ReadOnlyFileSystemException on a read-only file system");

        // Original file must still be present; moved name must not exist.
        assertTrue(nodeExistsInGraph("File", "pre-existing.txt"),
            "Original file must remain in Neo4J after failed move");
        assertFalse(nodeExistsInGraph("File", "moved.txt"),
            "Moved-to name must not appear in Neo4J after failed move");
    }

    // -------------------------------------------------------------------------
    // Read-only operations must continue to work
    // -------------------------------------------------------------------------

    /**
     * {@link Files#exists} must report {@code true} for paths that exist even when
     * the file system is read-only.
     */
    @Test
    @DisplayName("exists() returns true for existing paths on a read-only file system")
    void existsWorksOnReadOnly() {
        assertTrue(Files.exists(fileSystem.getPath(EXISTING_DIR)),
            "Files.exists must return true for /data on a read-only file system");
        assertTrue(Files.exists(fileSystem.getPath(EXISTING_FILE)),
            "Files.exists must return true for pre-existing.txt on a read-only file system");
        assertFalse(Files.exists(fileSystem.getPath("/nonexistent")),
            "Files.exists must return false for nonexistent path on a read-only file system");
    }

    /**
     * Reading basic file attributes (size, timestamps, isRegularFile, etc.) must
     * work without throwing an exception on a read-only file system.
     */
    @Test
    @DisplayName("readAttributes works on a read-only file system")
    void readAttributesWorksOnReadOnly() {
        assertDoesNotThrow(() -> {
            BasicFileAttributes attrs = Files.readAttributes(
                fileSystem.getPath(EXISTING_FILE), BasicFileAttributes.class);
            assertTrue(attrs.isRegularFile(),
                "Existing file must be reported as a regular file");
            assertNotNull(attrs.creationTime(),
                "Creation time must not be null");
        }, "readAttributes must not throw on a read-only file system");
    }

    /**
     * Verifies that the read-only flag does not cause any nodes to be added or
     * removed from the graph.  The total count of File and Directory nodes before
     * and after the blocked write attempts must be identical.
     */
    @Test
    @DisplayName("graph is unchanged after blocked write attempts on read-only file system")
    void graphUnchangedAfterBlockedWrites() {
        // Count nodes before.
        long before = countAllFileSystemNodes();

        // Attempt several writes (all must be blocked).
        try { Files.createDirectory(fileSystem.getPath("/blocked1")); } catch (Exception ignored) {}
        try { Files.createFile(fileSystem.getPath("/data/blocked.txt")); } catch (Exception ignored) {}
        try { Files.delete(fileSystem.getPath(EXISTING_FILE)); } catch (Exception ignored) {}

        // Count must be unchanged.
        assertEquals(before, countAllFileSystemNodes(),
            "Neo4J node count must not change after blocked writes on a read-only file system");
    }
}
