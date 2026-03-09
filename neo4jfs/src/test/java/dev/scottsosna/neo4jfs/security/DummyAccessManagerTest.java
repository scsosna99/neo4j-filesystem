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
package dev.scottsosna.neo4jfs.security;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.AccessMode;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DummyAccessManager - verifies it always grants maximum permissions
 * and always succeeds access checks (dummy/wide-open security)
 */
class DummyAccessManagerTest {

    private DummyAccessManager accessManager;

    @BeforeEach
    void setUp() throws Exception {
        accessManager = new DummyAccessManager();

        // Set rootPermissions via reflection
        Field rootPermissionsField = DummyAccessManager.class.getDeclaredField("rootPermissions");
        rootPermissionsField.setAccessible(true);
        rootPermissionsField.set(accessManager, "---------");
    }

    // ========== Admin User/Group Tests ==========

    @Test
    void testGetAdminUser_ReturnsDummy() {
        assertEquals("dummy", accessManager.getAdminUser());
    }

    @Test
    void testGetAdminGroup_ReturnsDummy() {
        assertEquals("dummy", accessManager.getAdminGroup());
    }

    @Test
    void testIsAdminUser_AlwaysReturnsTrue() {
        assertTrue(accessManager.isAdminUser(), "DummyAccessManager should always return true for isAdminUser");
    }

    @Test
    void testUserName_ReturnsDummy() {
        assertEquals("dummy", accessManager.userName());
    }

    // ========== Permission Conversion Tests ==========

    @Test
    void testConvertPermissionsFromSet_ReturnsRootPermissions() {
        // Given - any set of permissions
        Set<PosixFilePermission> permissions = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
        );

        // When
        String result = accessManager.convertPermissions(permissions);

        // Then
        assertEquals("---------", result, "Should always return rootPermissions regardless of input");
    }

    @Test
    void testConvertPermissionsFromSet_AllPermissions() {
        // Given
        Set<PosixFilePermission> permissions = EnumSet.allOf(PosixFilePermission.class);

        // When
        String result = accessManager.convertPermissions(permissions);

        // Then
        assertEquals("---------", result, "Should ignore input and return rootPermissions");
    }

    @Test
    void testConvertPermissionsFromSet_NoPermissions() {
        // Given
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);

        // When
        String result = accessManager.convertPermissions(permissions);

        // Then
        assertEquals("---------", result, "Should return rootPermissions even with empty input");
    }

    @Test
    void testConvertPermissionsFromString_AlwaysReturnsAllPermissions() {
        // Given - various permission strings
        String[] permissionStrings = {
                "---------",
                "rwxrwxrwx",
                "rw-r--r--",
                "r--r--r--",
                "invalid",
                "",
                null
        };

        // When/Then - all should return all permissions
        for (String permString : permissionStrings) {
            Set<PosixFilePermission> result = accessManager.convertPermissions(permString);

            assertNotNull(result, "Should never return null");
            assertEquals(9, result.size(), "Should always return all 9 permissions");
            assertTrue(result.containsAll(EnumSet.allOf(PosixFilePermission.class)),
                    "Should always return complete set of permissions for: " + permString);
        }
    }

    // ========== Access Check Tests ==========

    @Test
    void testCheckAccess_AlwaysSucceeds() throws Exception {
        // Given - entry with no permissions
        BaseEntry entry = createEntry("someuser", "somegroup", "---------");

        // When
        AccessMode[] result = accessManager.checkAccess(entry, AccessMode.READ, AccessMode.WRITE, AccessMode.EXECUTE);

        // Then
        assertNotNull(result);
        assertEquals(0, result.length, "Should always return empty array indicating success");
    }

    @Test
    void testCheckAccess_AllAccessModes() throws Exception {
        // Given
        BaseEntry entry = createEntry("owner", "group", "---rwx---");

        // When
        AccessMode[] resultRead = accessManager.checkAccess(entry, AccessMode.READ);
        AccessMode[] resultWrite = accessManager.checkAccess(entry, AccessMode.WRITE);
        AccessMode[] resultExecute = accessManager.checkAccess(entry, AccessMode.EXECUTE);
        AccessMode[] resultAll = accessManager.checkAccess(entry, AccessMode.READ, AccessMode.WRITE, AccessMode.EXECUTE);

        // Then - all should succeed
        assertEquals(0, resultRead.length);
        assertEquals(0, resultWrite.length);
        assertEquals(0, resultExecute.length);
        assertEquals(0, resultAll.length);
    }

    @Test
    void testCheckAccess_NoAccessModes() throws Exception {
        // Given
        BaseEntry entry = createEntry("owner", "group", "---------");

        // When
        AccessMode[] result = accessManager.checkAccess(entry);

        // Then
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testCheckAccess_NullEntry() {
        // Given
        BaseEntry entry = null;

        // When
        AccessMode[] result = accessManager.checkAccess(entry, AccessMode.READ);

        // Then
        assertNotNull(result);
        assertEquals(0, result.length, "Should succeed even with null entry");
    }

    // ========== Validation Tests ==========

    @Test
    void testValidatePermissions_AlwaysReturnsTrue() {
        // All inputs should be valid
        assertTrue(accessManager.validatePermissions("rwxrwxrwx"));
        assertTrue(accessManager.validatePermissions("---------"));
        assertTrue(accessManager.validatePermissions("invalid"));
        assertTrue(accessManager.validatePermissions(""));
        assertTrue(accessManager.validatePermissions("abc123"));
        assertTrue(accessManager.validatePermissions("too short"));
        assertTrue(accessManager.validatePermissions("way too long to be a valid permission string"));
        assertTrue(accessManager.validatePermissions(null));
    }

    // ========== Root Permissions Test ==========

    @Test
    void testRootPermissions_ReturnsConfiguredValue() {
        String result = accessManager.rootPermissions();
        assertEquals("---------", result);
    }

    // ========== Integration Test - Verify Wide-Open Behavior ==========

    @Test
    void testWideOpenBehavior_CompleteWorkflow() throws Exception {
        // This test verifies the complete "dummy" behavior: always permissive

        // 1. Any permission string converts to all permissions
        Set<PosixFilePermission> perms = accessManager.convertPermissions("r--------");
        assertEquals(9, perms.size(), "Should grant all permissions regardless of input");

        // 2. Any permission set converts to rootPermissions
        String permString = accessManager.convertPermissions(Set.of(PosixFilePermission.OWNER_READ));
        assertEquals("---------", permString);

        // 3. Access checks always succeed
        BaseEntry restrictedEntry = createEntry("owner", "group", "---------");
        AccessMode[] failedModes = accessManager.checkAccess(restrictedEntry, AccessMode.READ, AccessMode.WRITE, AccessMode.EXECUTE);
        assertEquals(0, failedModes.length, "All access checks should pass");

        // 4. Validation always succeeds
        assertTrue(accessManager.validatePermissions("completely invalid!@#$%"));

        // 5. Admin checks always succeed
        assertTrue(accessManager.isAdminUser());
        assertEquals("dummy", accessManager.userName());
        assertEquals("dummy", accessManager.getAdminUser());
        assertEquals("dummy", accessManager.getAdminGroup());
    }

    // ========== Helper Methods ==========

    private BaseEntry createEntry(String owner, String group, String permissions) throws Exception {
        FileEntry entry = new FileEntry();
        entry.setOwnerUserName(owner);
        entry.setOwnerGroupName(group);

        Field permissionsField = BaseEntry.class.getDeclaredField("permissions");
        permissionsField.setAccessible(true);
        permissionsField.set(entry, permissions);

        return entry;
    }
}
