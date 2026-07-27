/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BaseEntryRepositoryImpl shared functionality
 */
@ExtendWith(MockitoExtension.class)
class BaseEntryRepositoryImplTest {

    @Mock
    private Neo4jfsConfiguration config;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Result result;

    private BaseEntryRepositoryImpl repository;

    private URI testUri;

    @BeforeEach
    void setUp() {
        repository = new BaseEntryRepositoryImpl(config, sessionFactory);
        testUri = URI.create("neo4jfs://testdb/some/path");

        // Lenient stubbing since not all tests use the session factory
        lenient().when(sessionFactory.openSession()).thenReturn(session);
    }

    @Test
    void testFindNamedChild_ReturnsChildWhenFound() {
        // Given
        String parentId = "parent-123";
        String childName = "child.txt";

        DirectoryEntry parent = new DirectoryEntry();
        parent.setId(parentId);
        parent.setName("parent");

        FileEntry child = new FileEntry();
        child.setId("child-456");
        child.setName(childName);

        List<BaseEntry> mockResults = Arrays.asList(parent, child);
        when(session.query(eq(BaseEntry.class), anyString(), anyMap())).thenReturn(mockResults);

        // When
        BaseEntry foundChild = repository.findNamedChild(testUri, parentId, childName);

        // Then
        verify(sessionFactory).openSession();
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(session).query(eq(BaseEntry.class), anyString(), paramsCaptor.capture());

        Map<String, Object> capturedParams = paramsCaptor.getValue();
        assertEquals(parentId, capturedParams.get("id"));
        assertEquals(childName, capturedParams.get("name"));

        assertNotNull(foundChild);
        assertEquals(child.getId(), foundChild.getId());
        assertEquals(childName, foundChild.getName());
    }

    @Test
    void testFindNamedChild_ReturnsNullWhenNotFound() {
        // Given
        String parentId = "parent-123";
        String childName = "nonexistent.txt";
        List<BaseEntry> emptyResults = List.of();

        when(session.query(eq(BaseEntry.class), anyString(), anyMap())).thenReturn(emptyResults);

        // When
        BaseEntry foundChild = repository.findNamedChild(testUri, parentId, childName);

        // Then
        assertNull(foundChild);
    }

    @Test
    void testSave_SetsTimestampsForNewEntry() {
        // Given
        FileEntry newFile = new FileEntry();
        newFile.setName("newfile.txt");

        // When
        repository.save(testUri, newFile, FileEntry.class);

        // Then
        verify(sessionFactory).openSession();
        verify(session).save(newFile);

        assertNotNull(newFile.getCreated(), "Created timestamp should be set");
        assertNotNull(newFile.getLastModified(), "Last modified timestamp should be set");
        assertNotNull(newFile.getLastAccessed(), "Last accessed timestamp should be set");
    }

    @Test
    void testSave_UpdatesLastModifiedForExistingEntry() {
        // Given
        FileEntry existingFile = new FileEntry();
        existingFile.setId("existing-123");
        existingFile.setName("existing.txt");
        Instant created = Instant.now().minusSeconds(3600);
        existingFile.setCreated(created);

        // When
        repository.save(testUri, existingFile, FileEntry.class);

        // Then
        verify(session).save(existingFile);
        assertEquals(created, existingFile.getCreated(), "Created timestamp should not change");
        assertNotNull(existingFile.getLastModified(), "Last modified should be updated");
    }

    @Test
    void testSaveMultiple_ExecutesWithinTransaction() throws Exception {
        // Given
        org.neo4j.ogm.transaction.Transaction mockTransaction = mock(org.neo4j.ogm.transaction.Transaction.class);
        when(session.beginTransaction()).thenReturn(mockTransaction);

        Callable<Void> task1 = mock(Callable.class);
        Callable<Void> task2 = mock(Callable.class);
        List<Callable> tasks = Arrays.asList(task1, task2);

        // When
        repository.save(testUri, tasks);

        // Then
        verify(sessionFactory).openSession();
        verify(session).beginTransaction();
        verify(task1).call();
        verify(task2).call();
        verify(mockTransaction).commit();
    }

    @Test
    void testSaveMultiple_ThrowsExceptionOnFailure() {
        // Given
        org.neo4j.ogm.transaction.Transaction mockTransaction = mock(org.neo4j.ogm.transaction.Transaction.class);
        when(session.beginTransaction()).thenReturn(mockTransaction);

        Callable<Void> failingTask = () -> {
            throw new RuntimeException("Task failed");
        };
        List<Callable> tasks = List.of(failingTask);

        // When/Then
        assertThrows(IOException.class, () -> repository.save(testUri, tasks));
        verify(mockTransaction, never()).commit();
    }

    @Test
    void testDeleteRelationship_ReturnsCountWhenDeleted() {
        // Given
        String startId = "start-123";
        String endId = "end-456";

        org.neo4j.ogm.model.QueryStatistics queryStats = mock(org.neo4j.ogm.model.QueryStatistics.class);
        when(queryStats.getRelationshipsDeleted()).thenReturn(1);
        when(result.queryStatistics()).thenReturn(queryStats);
        when(session.query(anyString(), anyMap())).thenReturn(result);

        // When
        Integer deleted = repository.deleteRelationship(testUri, startId, endId);

        // Then
        verify(sessionFactory).openSession();
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(session).query(anyString(), paramsCaptor.capture());

        Map<String, String> capturedParams = paramsCaptor.getValue();
        assertEquals(startId, capturedParams.get("startId"));
        assertEquals(endId, capturedParams.get("endId"));

        assertEquals(1, deleted);
    }

    @Test
    void testDeleteRelationship_ReturnsZeroWhenNotFound() {
        // Given
        String startId = "start-123";
        String endId = "nonexistent-456";

        org.neo4j.ogm.model.QueryStatistics queryStats = mock(org.neo4j.ogm.model.QueryStatistics.class);
        when(queryStats.getRelationshipsDeleted()).thenReturn(0);
        when(result.queryStatistics()).thenReturn(queryStats);
        when(session.query(anyString(), anyMap())).thenReturn(result);

        // When
        Integer deleted = repository.deleteRelationship(testUri, startId, endId);

        // Then
        assertEquals(0, deleted);
    }

    @Test
    void testUpdateLastAccessed_LoadsAndUpdatesEntry() {
        // Given
        String entryId = "entry-123";
        FileEntry originalEntry = new FileEntry();
        originalEntry.setId(entryId);
        originalEntry.setName("test.txt");
        Instant originalAccessed = Instant.now().minusSeconds(3600);
        originalEntry.setLastAccessed(originalAccessed);

        FileEntry loadedEntry = new FileEntry();
        loadedEntry.setId(entryId);
        loadedEntry.setName("test.txt");
        loadedEntry.setLastAccessed(originalAccessed);

        when(session.load(FileEntry.class, entryId)).thenReturn(loadedEntry);

        // When
        repository.updateLastAccessed(testUri, originalEntry, FileEntry.class);

        // Then
        verify(sessionFactory).openSession();
        verify(session).load(FileEntry.class, entryId);
        verify(session).save(loadedEntry);

        assertNotEquals(originalAccessed, loadedEntry.getLastAccessed(),
                "Last accessed should be updated to current time");
    }

    @Test
    void testPrepareEntry_SetsTransientFields() throws Exception {
        // Given
        DirectoryEntry parent = new DirectoryEntry();
        parent.setId("parent-123");
        parent.setName("parent");
        setPermissionsViaReflection(parent, "rwxr-xr-x");

        FileEntry child = new FileEntry();
        child.setId("child-456");
        child.setName("child.txt");

        // When
        FileEntry prepared = repository.prepareEntry(child, testUri, parent);

        // Then
        assertNotNull(prepared);
        assertEquals(testUri, prepared.getFsUri(), "FsUri should be set");
        assertEquals("rwxr-xr-x", prepared.getInheritedPermissions(),
                "Inherited permissions should be derived from parent");
    }

    @Test
    void testPrepareEntriesSiblings_PreparesAllWithSameParent() throws Exception {
        // Given
        DirectoryEntry parent = new DirectoryEntry();
        parent.setId("parent-123");
        setPermissionsViaReflection(parent, "rwxr-xr-x");

        FileEntry file1 = new FileEntry();
        file1.setName("file1.txt");

        FileEntry file2 = new FileEntry();
        file2.setName("file2.txt");

        List<FileEntry> siblings = Arrays.asList(file1, file2);

        // When
        List<FileEntry> prepared = repository.prepareEntriesSiblings(siblings, testUri, parent);

        // Then
        assertEquals(2, prepared.size());
        prepared.forEach(entry -> {
            assertEquals(testUri, entry.getFsUri());
            assertEquals("rwxr-xr-x", entry.getInheritedPermissions());
        });
    }

    @Test
    void testPrepareEntriesTree_PreparesPathHierarchy() throws Exception {
        // Given
        DirectoryEntry root = new DirectoryEntry();
        root.setId("root");
        root.setName("/");
        root.setRoot(true);
        setPermissionsViaReflection(root, "rwxr-xr-x");

        DirectoryEntry dir1 = new DirectoryEntry();
        dir1.setId("dir1");
        dir1.setName("dir1");
        setPermissionsViaReflection(dir1, "rwxr-xr-x");

        FileEntry file = new FileEntry();
        file.setId("file1");
        file.setName("file.txt");

        List<BaseEntry> tree = Arrays.asList(root, dir1, file);

        // When
        List<BaseEntry> prepared = repository.prepareEntriesTree(tree, testUri);

        // Then
        assertEquals(3, prepared.size());
        // Root should have URI set and no inherited permissions (it has its own)
        assertEquals(testUri, root.getFsUri());
        // dir1 should inherit from root
        assertEquals(testUri, dir1.getFsUri());
        // file should inherit from dir1
        assertEquals(testUri, file.getFsUri());
        assertEquals("rwxr-xr-x", file.getInheritedPermissions());
    }

    /**
     * Helper method to set permissions directly via reflection, bypassing Spring dependency
     * in PosixFilePermissionConverter which requires ApplicationContext
     */
    private void setPermissionsViaReflection(BaseEntry entry, String permissions) throws Exception {
        Field permissionsField = BaseEntry.class.getDeclaredField("permissions");
        permissionsField.setAccessible(true);
        permissionsField.set(entry, permissions);
    }
}
