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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.nio.file.AccessMode;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PosixAccessManager - verifies seamless conversion between
 * Set<PosixFilePermission> and Unix ls-style permission strings
 */
class PosixAccessManagerTest {

    private PosixAccessManager accessManager;

    @BeforeEach
    void setUp() throws Exception {
        accessManager = new PosixAccessManager();

        // Set rootPermissions via reflection
        Field rootPermissionsField = PosixAccessManager.class.getDeclaredField("rootPermissions");
        rootPermissionsField.setAccessible(true);
        rootPermissionsField.set(accessManager, "rwxr-xr-x");

        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    // ========== Permission String Conversion Tests ==========

    @Test
    void testConvertPermissions_AllPermissionsSet() {
        // Given - all permissions
        Set<PosixFilePermission> permissions = EnumSet.allOf(PosixFilePermission.class);

        // When
        String result = accessManager.convertPermissions(permissions);

        // Then
        assertEquals("rwxrwxrwx", result, "All permissions should convert to rwxrwxrwx");
    }

    @Test
    void testConvertPermissions_NoPermissionsSet() {
        // Given - no permissions
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);

        // When
        String result = accessManager.convertPermissions(permissions);

        // Then
        assertEquals("---------", result, "No permissions should convert to ---------");
    }

    @Test
    void testConvertPermissions_OwnerReadWriteExecute() {
        // Given
        Set<PosixFilePermission> permissions = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
        );

        // When
        String result = accessManager.convertPermissions(permissions);

        // Then
        assertEquals("rwx------", result, "Owner rwx should convert to rwx------");
    }

    @Test
    void testConvertPermissions_GroupReadExecute() {
        // Given
        Set<PosixFilePermission> permissions = Set.of(
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE
        );

        // When
        String result = accessManager.convertPermissions(permissions);

        // Then
        assertEquals("---r-x---", result, "Group rx should convert to ---r-x---");
    }

    @Test
    void testConvertPermissions_OthersReadOnly() {
        // Given
        Set<PosixFilePermission> permissions = Set.of(
                PosixFilePermission.OTHERS_READ
        );

        // When
        String result = accessManager.convertPermissions(permissions);

        // Then
        assertEquals("------r--", result, "Others read should convert to ------r--");
    }

    @Test
    void testConvertPermissions_TypicalFilePermissions() {
        // Given - typical file: rw-r--r--
        Set<PosixFilePermission> permissions = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ
        );

        // When
        String result = accessManager.convertPermissions(permissions);

        // Then
        assertEquals("rw-r--r--", result, "Typical file permissions should be rw-r--r--");
    }

    @Test
    void testConvertPermissions_TypicalDirectoryPermissions() {
        // Given - typical directory: rwxr-xr-x
        Set<PosixFilePermission> permissions = Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
        );

        // When
        String result = accessManager.convertPermissions(permissions);

        // Then
        assertEquals("rwxr-xr-x", result, "Typical directory permissions should be rwxr-xr-x");
    }

    // ========== Reverse Conversion Tests ==========

    @Test
    void testConvertPermissionsFromString_AllPermissions() {
        // When
        Set<PosixFilePermission> result = accessManager.convertPermissions("rwxrwxrwx");

        // Then
        assertEquals(9, result.size());
        assertTrue(result.containsAll(EnumSet.allOf(PosixFilePermission.class)));
    }

    @Test
    void testConvertPermissionsFromString_NoPermissions() {
        // When
        Set<PosixFilePermission> result = accessManager.convertPermissions("---------");

        // Then
        assertEquals(0, result.size());
    }

    @Test
    void testConvertPermissionsFromString_OwnerOnly() {
        // When
        Set<PosixFilePermission> result = accessManager.convertPermissions("rwx------");

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains(PosixFilePermission.OWNER_READ));
        assertTrue(result.contains(PosixFilePermission.OWNER_WRITE));
        assertTrue(result.contains(PosixFilePermission.OWNER_EXECUTE));
    }

    @Test
    void testConvertPermissionsFromString_MixedPermissions() {
        // When
        Set<PosixFilePermission> result = accessManager.convertPermissions("rw-r-xr--");

        // Then
        assertEquals(5, result.size());
        assertTrue(result.contains(PosixFilePermission.OWNER_READ));
        assertTrue(result.contains(PosixFilePermission.OWNER_WRITE));
        assertTrue(result.contains(PosixFilePermission.GROUP_READ));
        assertTrue(result.contains(PosixFilePermission.GROUP_EXECUTE));
        assertTrue(result.contains(PosixFilePermission.OTHERS_READ));
    }

    @Test
    void testConvertPermissionsFromString_NullReturnsEmpty() {
        // When
        Set<PosixFilePermission> result = accessManager.convertPermissions((String) null);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testConvertPermissionsFromString_EmptyReturnsEmpty() {
        // When
        Set<PosixFilePermission> result = accessManager.convertPermissions("");

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testConvertPermissionsFromString_InvalidThrowsException() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            accessManager.convertPermissions("invalid");
        });
    }

    // ========== Round-trip Conversion Tests ==========

    @Test
    void testRoundTripConversion_AllPermissions() {
        // Given
        Set<PosixFilePermission> original = EnumSet.allOf(PosixFilePermission.class);

        // When
        String asString = accessManager.convertPermissions(original);
        Set<PosixFilePermission> backToSet = accessManager.convertPermissions(asString);

        // Then
        assertEquals(original, backToSet);
    }

    @Test
    void testRoundTripConversion_VariousPermissions() {
        // Test several common permission sets
        testRoundTrip(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        testRoundTrip(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        testRoundTrip(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));
        testRoundTrip(EnumSet.noneOf(PosixFilePermission.class));
    }

    private void testRoundTrip(Set<PosixFilePermission> original) {
        String asString = accessManager.convertPermissions(original);
        Set<PosixFilePermission> backToSet = accessManager.convertPermissions(asString);
        assertEquals(original, backToSet, "Round-trip conversion should preserve permissions");
    }

    // ========== Validation Tests ==========

    @Test
    void testValidatePermissions_ValidFormats() {
        assertTrue(accessManager.validatePermissions("rwxrwxrwx"));
        assertTrue(accessManager.validatePermissions("---------"));
        assertTrue(accessManager.validatePermissions("rwxr-xr-x"));
        assertTrue(accessManager.validatePermissions("rw-r--r--"));
        assertTrue(accessManager.validatePermissions("r--r--r--"));
    }

    @Test
    void testValidatePermissions_InvalidFormats() {
        assertFalse(accessManager.validatePermissions("rwxrwxrw")); // Too short
        assertFalse(accessManager.validatePermissions("rwxrwxrwxx")); // Too long
        assertFalse(accessManager.validatePermissions("rwxrwxrwa")); // Invalid character
        assertFalse(accessManager.validatePermissions(""));
        assertFalse(accessManager.validatePermissions("invalid"));
    }

    // ========== Access Check Tests ==========

    @Test
    void testCheckAccess_AdminUserAlwaysHasAccess() throws Exception {
        // Given - root user authenticated
        setAuthenticatedUser("root", List.of("wheel"));

        BaseEntry entry = createEntry("owner", "group", "---r-----");

        // When
        AccessMode[] result = accessManager.checkAccess(entry, AccessMode.READ, AccessMode.WRITE, AccessMode.EXECUTE);

        // Then
        assertEquals(0, result.length, "Admin user should have all access");
    }

    @Test
    void testCheckAccess_OwnerHasAccess() throws Exception {
        // Given - user authenticated as owner
        setAuthenticatedUser("owner", List.of("somegroup"));

        BaseEntry entry = createEntry("owner", "group", "rwx------");

        // When
        AccessMode[] result = accessManager.checkAccess(entry, AccessMode.READ, AccessMode.WRITE, AccessMode.EXECUTE);

        // Then
        assertEquals(0, result.length, "Owner with rwx should have all access");
    }

    @Test
    void testCheckAccess_OwnerLacksWriteAccess() throws Exception {
        // Given
        setAuthenticatedUser("owner", List.of("somegroup"));

        BaseEntry entry = createEntry("owner", "group", "r-x------");

        // When
        AccessMode[] result = accessManager.checkAccess(entry, AccessMode.READ, AccessMode.WRITE, AccessMode.EXECUTE);

        // Then
        assertEquals(1, result.length);
        assertEquals(AccessMode.WRITE, result[0], "Should fail write check");
    }

    @Test
    void testCheckAccess_GroupHasAccess() throws Exception {
        // Given - user in the owning group
        setAuthenticatedUser("user", List.of("owninggroup"));

        BaseEntry entry = createEntry("owner", "owninggroup", "---rwx---");

        // When
        AccessMode[] result = accessManager.checkAccess(entry, AccessMode.READ, AccessMode.WRITE, AccessMode.EXECUTE);

        // Then
        assertEquals(0, result.length, "Group member should have access");
    }

    @Test
    void testCheckAccess_OthersHaveReadOnlyAccess() throws Exception {
        // Given - user not owner and not in group
        setAuthenticatedUser("randomuser", List.of("randomgroup"));

        BaseEntry entry = createEntry("owner", "group", "------r--");

        // When
        AccessMode[] failedRead = accessManager.checkAccess(entry, AccessMode.READ);
        AccessMode[] failedWrite = accessManager.checkAccess(entry, AccessMode.WRITE);
        AccessMode[] failedExecute = accessManager.checkAccess(entry, AccessMode.EXECUTE);

        // Then
        assertEquals(0, failedRead.length, "Others should have read access");
        assertEquals(1, failedWrite.length, "Others should not have write access");
        assertEquals(1, failedExecute.length, "Others should not have execute access");
    }

    @Test
    void testCheckAccess_NoAuthentication() throws Exception {
        // Given - no authentication (defaults to "nobody")
        SecurityContextHolder.clearContext();

        BaseEntry entry = createEntry("owner", "group", "------r--");

        // When
        AccessMode[] result = accessManager.checkAccess(entry, AccessMode.READ);

        // Then
        assertEquals(0, result.length, "Should use 'others' permissions when unauthenticated");
    }

    @Test
    void testCheckAccess_InheritedPermissions() throws Exception {
        // Given
        setAuthenticatedUser("owner", List.of("group"));

        FileEntry entry = new FileEntry();
        entry.setOwnerUserName("owner");
        entry.setOwnerGroupName("group");
        // No direct permissions, but has inherited
        setInheritedPermissionsViaReflection(entry, "rwxr-xr-x");

        // When
        AccessMode[] result = accessManager.checkAccess(entry, AccessMode.READ, AccessMode.WRITE, AccessMode.EXECUTE);

        // Then
        assertEquals(0, result.length, "Should use inherited permissions when no direct permissions");
    }

    // ========== Utility Method Tests ==========

    @Test
    void testGetAdminUser() {
        assertEquals("root", accessManager.getAdminUser());
    }

    @Test
    void testGetAdminGroup() {
        assertEquals("wheel", accessManager.getAdminGroup());
    }

    @Test
    void testIsAdminUser_WithRootUser() {
        setAuthenticatedUser("root", List.of("wheel"));
        assertTrue(accessManager.isAdminUser());
    }

    @Test
    void testIsAdminUser_WithRegularUser() {
        setAuthenticatedUser("user", List.of("users"));
        assertFalse(accessManager.isAdminUser());
    }

    @Test
    void testUserName() {
        setAuthenticatedUser("testuser", List.of("testgroup"));
        assertEquals("testuser", accessManager.userName());
    }

    @Test
    void testUserName_Unauthenticated() {
        SecurityContextHolder.clearContext();
        assertEquals("nobody", accessManager.userName());
    }

    @Test
    void testRootPermissions() {
        String result = accessManager.rootPermissions();
        assertEquals("rwxr-xr-x", result);
    }

    // ========== Helper Methods ==========

    private void setAuthenticatedUser(String username, List<String> groups) {
        List<SimpleGrantedAuthority> authorities = groups.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private BaseEntry createEntry(String owner, String group, String permissions) throws Exception {
        FileEntry entry = new FileEntry();
        entry.setOwnerUserName(owner);
        entry.setOwnerGroupName(group);
        setPermissionsViaReflection(entry, permissions);
        return entry;
    }

    private void setPermissionsViaReflection(BaseEntry entry, String permissions) throws Exception {
        Field permissionsField = BaseEntry.class.getDeclaredField("permissions");
        permissionsField.setAccessible(true);
        permissionsField.set(entry, permissions);
    }

    private void setInheritedPermissionsViaReflection(BaseEntry entry, String permissions) throws Exception {
        Field inheritedField = BaseEntry.class.getDeclaredField("inheritedPermissions");
        inheritedField.setAccessible(true);
        inheritedField.set(entry, permissions);
    }
}
