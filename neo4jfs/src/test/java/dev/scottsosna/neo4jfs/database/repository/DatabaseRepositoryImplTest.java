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
import dev.scottsosna.neo4jfs.database.model.neo4j.Database;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DatabaseRepositoryImpl
 */
@ExtendWith(MockitoExtension.class)
class DatabaseRepositoryImplTest {

    @Mock
    private Neo4jfsConfiguration config;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Result result;

    private DatabaseRepositoryImpl repository;

    private URI testUri;

    @BeforeEach
    void setUp() {
        repository = new DatabaseRepositoryImpl(config, sessionFactory);
        testUri = URI.create("neo4jfs://testdb/some/path");

        // Setup default mock behavior
        when(sessionFactory.openSession()).thenReturn(session);
    }

    @Test
    void testCreate_CreatesNewDatabase() {
        // Given
        String dbName = "testdb";
        Map<String, Object> dbResultMap = createDatabaseResultMap(dbName);
        List<Map<String, Object>> resultList = List.of(dbResultMap);

        when(session.query(anyString(), anyMap())).thenReturn(result);
        when(result.spliterator()).thenReturn(resultList.spliterator());

        // When
        Database created = repository.create(testUri);

        // Then
        // create() calls query() for CREATE DATABASE, createIndexes() opens session and creates 2 indexes,
        // then query() is called again for SHOW DATABASE to return the created database
        verify(sessionFactory, atLeast(3)).openSession();
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(session, atLeastOnce()).query(anyString(), paramsCaptor.capture());

        // Verify CREATE DATABASE was called with correct database name
        List<Map<String, String>> allParams = paramsCaptor.getAllValues();
        assertTrue(allParams.stream().anyMatch(p -> dbName.equals(p.get("database"))),
                "Should call query with database parameter");

        assertNotNull(created);
        assertEquals(dbName, created.getName());
    }

    @Test
    void testCreateIndexes_CreatesRequiredIndexes() {
        // When
        repository.createIndexes(testUri);

        // Then
        verify(sessionFactory).openSession();
        // Verify two indexes are created (ENTRY_NAME_IDX and FILE_STORAGE_IDX)
        verify(session, times(2)).query(contains("CREATE INDEX"), anyMap());
    }

    @Test
    void testDrop_DropsDatabase() {
        // Given
        String dbName = testUri.getHost();

        when(session.query(anyString(), anyMap())).thenReturn(result);

        // When
        repository.drop(testUri);

        // Then
        verify(sessionFactory).openSession();
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(session).query(contains("DROP DATABASE"), paramsCaptor.capture());

        Map<String, String> capturedParams = paramsCaptor.getValue();
        assertEquals(dbName, capturedParams.get("database"));
    }

    @Test
    void testFindByUri_ReturnsDatabase() {
        // Given
        String dbName = testUri.getHost();
        Map<String, Object> dbResultMap = createDatabaseResultMap(dbName);
        List<Map<String, Object>> resultList = List.of(dbResultMap);

        when(session.query(anyString(), anyMap())).thenReturn(result);
        when(result.spliterator()).thenReturn(resultList.spliterator());

        // When
        Database found = repository.find(testUri);

        // Then
        verify(sessionFactory).openSession();
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(session).query(contains("SHOW DATABASE"), paramsCaptor.capture());

        Map<String, String> capturedParams = paramsCaptor.getValue();
        assertEquals(dbName, capturedParams.get("database"));

        assertNotNull(found);
        assertEquals(dbName, found.getName());
    }

    @Test
    void testFindByUri_ReturnsNullWhenNotFound() {
        // Given
        List<Map<String, Object>> emptyList = List.of();

        when(session.query(anyString(), anyMap())).thenReturn(result);
        when(result.spliterator()).thenReturn(emptyList.spliterator());

        // When
        Database found = repository.find(testUri);

        // Then
        assertNull(found);
    }

    @Test
    void testFindByName_ReturnsDatabase() {
        // Given
        String dbName = "mydb";
        Map<String, Object> dbResultMap = createDatabaseResultMap(dbName);
        List<Map<String, Object>> resultList = List.of(dbResultMap);

        when(session.query(anyString(), anyMap())).thenReturn(result);
        when(result.spliterator()).thenReturn(resultList.spliterator());

        // When
        Database found = repository.find(dbName);

        // Then
        verify(sessionFactory).openSession();
        ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(session).query(contains("SHOW DATABASE"), paramsCaptor.capture());

        Map<String, String> capturedParams = paramsCaptor.getValue();
        assertEquals(dbName, capturedParams.get("database"));

        assertNotNull(found);
        assertEquals(dbName, found.getName());
    }

    @Test
    void testFindByName_ReturnsNullWhenNotFound() {
        // Given
        String dbName = "nonexistent";
        List<Map<String, Object>> emptyList = List.of();

        when(session.query(anyString(), anyMap())).thenReturn(result);
        when(result.spliterator()).thenReturn(emptyList.spliterator());

        // When
        Database found = repository.find(dbName);

        // Then
        assertNull(found);
    }

    @Test
    void testFindAll_ReturnsAllDatabases() {
        // Given
        Map<String, Object> db1 = createDatabaseResultMap("db1");
        Map<String, Object> db2 = createDatabaseResultMap("db2");
        Map<String, Object> db3 = createDatabaseResultMap("db3");
        List<Map<String, Object>> resultList = List.of(db1, db2, db3);

        when(session.query(anyString(), anyMap())).thenReturn(result);
        when(result.spliterator()).thenReturn(resultList.spliterator());

        // When
        List<Database> databases = repository.findAll();

        // Then
        verify(sessionFactory).openSession();
        verify(session).query(eq("SHOW DATABASES"), anyMap());

        assertNotNull(databases);
        assertEquals(3, databases.size());
        assertEquals("db1", databases.get(0).getName());
        assertEquals("db2", databases.get(1).getName());
        assertEquals("db3", databases.get(2).getName());
    }

    @Test
    void testFindAll_ReturnsEmptyListWhenNoDatabases() {
        // Given
        List<Map<String, Object>> emptyList = List.of();

        when(session.query(anyString(), anyMap())).thenReturn(result);
        when(result.spliterator()).thenReturn(emptyList.spliterator());

        // When
        List<Database> databases = repository.findAll();

        // Then
        assertNotNull(databases);
        assertTrue(databases.isEmpty());
    }

    /**
     * Helper method to create a mock database result map as returned by Neo4j SHOW DATABASE command
     */
    private Map<String, Object> createDatabaseResultMap(String name) {
        Map<String, Object> map = new HashMap<>();
        map.put("access", "read-write");
        map.put("address", "localhost:7687");
        map.put("role", "primary");
        map.put("currentStatus", "online");
        map.put("type", "standard");
        map.put("statusMessage", "");
        map.put("requestedStatus", "online");
        map.put("home", "false");
        map.put("default", "false");
        map.put("name", name);
        map.put("writer", "true");
        return map;
    }
}
