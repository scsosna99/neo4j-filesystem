/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.database.repository;

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
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

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FileEntryRepositoryImpl
 */
@ExtendWith(MockitoExtension.class)
class FileEntryRepositoryImplTest {

    @Mock
    private Neo4jfsConfiguration config;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Result result;

    private FileEntryRepositoryImpl repository;

    private URI testUri;

    @BeforeEach
    void setUp() {
        // Use test constructor to inject mocked SessionFactory
        repository = new FileEntryRepositoryImpl(config, sessionFactory);
        testUri = URI.create("neo4jfs://testdb/some/path");

        // Setup default mock behavior
        when(sessionFactory.openSession()).thenReturn(session);
    }

    @Test
    void testCreate_SavesFileEntryAndSetsTimestamps() {
        // Given
        FileEntry fileEntry = new FileEntry();
        fileEntry.setName("test.txt");
        fileEntry.setStorageId("storage-123");
        fileEntry.setSize(1024L);

        // When
        FileEntry result = repository.create(testUri, fileEntry);

        // Then
        verify(sessionFactory).openSession();
        verify(session).save(fileEntry);
        assertNotNull(result);
        assertNotNull(fileEntry.getCreated(), "Created timestamp should be set");
        assertNotNull(fileEntry.getLastModified(), "Last modified timestamp should be set");
        assertNotNull(fileEntry.getLastAccessed(), "Last accessed timestamp should be set");
    }

    @Test
    void testCreate_UpdatesTimestampsOnExistingFile() {
        // Given
        FileEntry existingFile = new FileEntry();
        existingFile.setId("existing-id");
        existingFile.setName("existing.txt");
        existingFile.setCreated(Instant.now().minusSeconds(3600));
        Instant originalCreated = existingFile.getCreated();

        // When
        repository.create(testUri, existingFile);

        // Then
        verify(session).save(existingFile);
        assertEquals(originalCreated, existingFile.getCreated(), "Created timestamp should not change");
        assertNotNull(existingFile.getLastModified(), "Last modified should be updated");
    }

    @Test
    void testLoad_ReturnsFileEntryWithFsUri() {
        // Given
        String fileNodeId = "node-123";
        FileEntry expectedFile = new FileEntry();
        expectedFile.setId(fileNodeId);
        expectedFile.setName("test.txt");
        expectedFile.setStorageId("storage-123");

        when(session.load(FileEntry.class, fileNodeId)).thenReturn(expectedFile);

        // When
        FileEntry result = repository.load(testUri, fileNodeId);

        // Then
        verify(sessionFactory).openSession();
        verify(session).load(FileEntry.class, fileNodeId);
        assertNotNull(result);
        assertEquals(fileNodeId, result.getId());
        assertEquals("test.txt", result.getName());
        assertEquals(testUri, result.getFsUri(), "FsUri should be set by load method");
    }

    @Test
    void testFindByStorageId_ReturnsMatchingFiles() {
        // Given
        String storageId = "storage-123";
        FileEntry file1 = new FileEntry();
        file1.setId("file-1");
        file1.setStorageId(storageId);
        file1.setName("file1.txt");

        FileEntry file2 = new FileEntry();
        file2.setId("file-2");
        file2.setStorageId(storageId);
        file2.setName("file2.txt");

        Iterable<FileEntry> mockResults = Arrays.asList(file1, file2);
        when(session.query(eq(FileEntry.class), anyString(), anyMap())).thenReturn(mockResults);

        // When
        List<FileEntry> results = repository.findByStorageId(testUri, storageId);

        // Then
        verify(sessionFactory).openSession();
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(session).query(eq(FileEntry.class), anyString(), paramsCaptor.capture());

        Map<String, Object> capturedParams = paramsCaptor.getValue();
        assertEquals(storageId, capturedParams.get("storageId"));

        assertEquals(2, results.size());
        assertEquals("file-1", results.get(0).getId());
        assertEquals("file-2", results.get(1).getId());
    }

    @Test
    void testFindByStorageId_ReturnsEmptyListWhenNoMatches() {
        // Given
        String storageId = "nonexistent-storage";
        Iterable<FileEntry> emptyResults = List.of();
        when(session.query(eq(FileEntry.class), anyString(), anyMap())).thenReturn(emptyResults);

        // When
        List<FileEntry> results = repository.findByStorageId(testUri, storageId);

        // Then
        verify(session).query(eq(FileEntry.class), anyString(), anyMap());
        assertTrue(results.isEmpty());
    }

    @Test
    void testDelete_ReturnsTrue_WhenNodeDeleted() {
        // Given
        String fileNodeId = "node-123";
        org.neo4j.ogm.model.QueryStatistics queryStats = mock(org.neo4j.ogm.model.QueryStatistics.class);
        when(queryStats.getNodesDeleted()).thenReturn(1);
        when(result.queryStatistics()).thenReturn(queryStats);
        when(session.query(anyString(), anyMap())).thenReturn(result);

        // When
        boolean deleted = repository.delete(testUri, fileNodeId);

        // Then
        verify(sessionFactory).openSession();
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(session).query(anyString(), paramsCaptor.capture());

        Map<String, Object> capturedParams = paramsCaptor.getValue();
        assertEquals(fileNodeId, capturedParams.get("id"));
        assertTrue(deleted);
    }

    @Test
    void testDelete_ReturnsFalse_WhenNodeNotFound() {
        // Given
        String fileNodeId = "nonexistent-node";
        org.neo4j.ogm.model.QueryStatistics queryStats = mock(org.neo4j.ogm.model.QueryStatistics.class);
        when(queryStats.getNodesDeleted()).thenReturn(0);
        when(result.queryStatistics()).thenReturn(queryStats);
        when(session.query(anyString(), anyMap())).thenReturn(result);

        // When
        boolean deleted = repository.delete(testUri, fileNodeId);

        // Then
        verify(session).query(anyString(), anyMap());
        assertFalse(deleted);
    }
}
