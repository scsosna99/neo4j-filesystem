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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessMode;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(prefix = "neo4jfs", name = "security", havingValue = "posix")
public class PosixAccessManager implements AccessManager {

    /**
     * Default permissions applied when creating the root directory for a Neo4Jfs partition.  When unconfigured,
     * it's read-write-execute for owner and read-only for group (640).
     */
    @Value("${neo4jfs.security.posix.rootPermissions:rwxr-x---}")
    private String rootPermissions;

    /**
     * An empty array of {@code AccessMode}s indicates all checks have passed.
     */
    private static final AccessMode[] EMPTY_ACCESS_MODES = new AccessMode[0];

    /**
     * Using standard *nix admin group "wheel" for all uber-group permissioning.
     */
    public static final String NAME_ADMIN_GROUP = "wheel";

    /**
     * Using standard *nix admin user "root" for all uber-user permissioning.
     */
    public static final String NAME_ADMIN_USER = "root";

    /**
     * An unknown or unauthenticated group.
     */
    public static final String NAME_UNAUTHENTICATED_GROUP = "nobody";

    /**
     * An unknown or unauthenticated user.
     */
    public static final String NAME_UNAUTHENTICATED_USER = "nobody";
    /**
     *  Default user with presumably least permissions, used when no authenticated user is available via Spring security.
     */
    private static final Authentication unknownUnauthenticatedUser;
    static {
        unknownUnauthenticatedUser = new AnonymousAuthenticationToken(
            "anonymousUser",
            NAME_UNAUTHENTICATED_USER,
            List.of(new SimpleGrantedAuthority(NAME_UNAUTHENTICATED_GROUP)));
    }

    /**
     * Neo4Jfs permissions are stored as the 9-character string displayed by a Unix "ls -l" command.  This static
     * pattern can be used to validate that the permissions string is correctly formatted to prevent errors.
     */
    private static Pattern validation = Pattern.compile("^[r-][w-][x-][r-][w-][x-][r-][w-][x-]$");

    /**
     *  Posix permission strings to represent Neo4Jfs permissions.
     */
    public static final char NEO4JFS_PERMISSION_READ = 'r';
    public static final char NEO4JFS_PERMISSION_WRITE = 'w';
    public static final char NEO4JFS_PERMISSION_EXECUTE = 'x';
    public static final char NEO4JFS_PERMISSION_NONE = '-';
    public static final String NEO4JFS_PERMISSION_NONE_GROUP = "---";


    /**
     * Checks access of entry against the AccessModes provided.
     * @param entry the file/directory/entry to check permissions
     * @param modes requested access modes
     * @return an empty array if all checks pass, otherwise the {@code AccessMode}s that failed.
     */
    public AccessMode[] checkAccess(BaseEntry entry, AccessMode... modes) {
        if (modes == null || modes.length == 0) return modes;
        return checkAccessWork(
            entry.getOwnerUserName(),
            entry.getOwnerGroupName(),
            entry.getPermissions() != null ? entry.getPermissions() : entry.getInheritedPermissions(),
            modes);
    }

    /**
     * Convert set of {@link PosixFilePermission} to a (somewhat) human-readable string as displayed by *nix ls -l command
     * @param posixPermissions Posix permissions to convert
     * @return String representation of permissions stored with entry.
     */
    public String convertPermissions(final Set<PosixFilePermission> posixPermissions) {
        char[] toReturn = new char[9];
        Arrays.fill(toReturn, NEO4JFS_PERMISSION_NONE);

        for (PosixFilePermission one: posixPermissions) {
            switch (one) {
                case OWNER_READ: toReturn[0] = NEO4JFS_PERMISSION_READ; break;
                case OWNER_WRITE: toReturn[1] = NEO4JFS_PERMISSION_WRITE; break;
                case OWNER_EXECUTE: toReturn[2] = NEO4JFS_PERMISSION_EXECUTE; break;
                case GROUP_READ: toReturn[3] = NEO4JFS_PERMISSION_READ; break;
                case GROUP_WRITE: toReturn[4] = NEO4JFS_PERMISSION_WRITE; break;
                case GROUP_EXECUTE: toReturn[5] = NEO4JFS_PERMISSION_EXECUTE; break;
                case OTHERS_READ: toReturn[6] = NEO4JFS_PERMISSION_READ; break;
                case OTHERS_WRITE: toReturn[7] = NEO4JFS_PERMISSION_WRITE; break;
                case OTHERS_EXECUTE: toReturn[8] = NEO4JFS_PERMISSION_EXECUTE; break;
            }
        }

        return new String(toReturn);
    }

    /**
     * Convert human-readable string to set of {@link PosixFilePermission}
     * @param permissions permission string, most likely for a file/directory/entry.
     * @return corresponding set of Posix permissions.
     */
    public Set<PosixFilePermission> convertPermissions(final String permissions) {
        if (permissions == null || permissions.isEmpty()) return EnumSet.noneOf(PosixFilePermission.class);

        //  Make sure the permissions string is valid before proceeding.
        if (!validatePermissions(permissions)) {
            throw new IllegalArgumentException("Permissions string must be 9 characters long");
        }

        Set<PosixFilePermission> toReturn = EnumSet.noneOf(PosixFilePermission.class);
        char[] array = permissions.toCharArray();
        if (array[0] == NEO4JFS_PERMISSION_READ) toReturn.add(PosixFilePermission.OWNER_READ);
        if (array[1] == NEO4JFS_PERMISSION_WRITE) toReturn.add(PosixFilePermission.OWNER_WRITE);
        if (array[2] == NEO4JFS_PERMISSION_EXECUTE) toReturn.add(PosixFilePermission.OWNER_EXECUTE);
        if (array[3] == NEO4JFS_PERMISSION_READ) toReturn.add(PosixFilePermission.GROUP_READ);
        if (array[4] == NEO4JFS_PERMISSION_WRITE) toReturn.add(PosixFilePermission.GROUP_WRITE);
        if (array[5] == NEO4JFS_PERMISSION_EXECUTE) toReturn.add(PosixFilePermission.GROUP_EXECUTE);
        if (array[6] == NEO4JFS_PERMISSION_READ) toReturn.add(PosixFilePermission.OTHERS_READ);
        if (array[7] == NEO4JFS_PERMISSION_WRITE) toReturn.add(PosixFilePermission.OTHERS_WRITE);
        if (array[8] == NEO4JFS_PERMISSION_EXECUTE) toReturn.add(PosixFilePermission.OTHERS_EXECUTE);

        return Collections.unmodifiableSet(toReturn);
    }

    /**
     * @return the admin group for the AccessManager implementation.
     */
    public String getAdminGroup() {
        return NAME_ADMIN_GROUP;
    }

    /**
     * @return the admin user for the AccessManager implementation.
     */
    public String getAdminUser() {
        return NAME_ADMIN_USER;
    }

    /**
     * Is currently-authenticated user considered admin or super-user?
     * @return true if admin; false otherwise
     */
    public boolean isAdminUser() {

        //  Get current authentication and determine if user is admin.
        return NAME_ADMIN_USER.equals(determineAuthentication().getName());
    }

    /**
     * @return root directory permissions used when creating file system.
     */
    public String rootPermissions() {
        validatePermissions(rootPermissions);
        return rootPermissions;
    }

    /**
     * Validate the permission string against the regex.
     * @param permissions the permissions string to be validated
     * @return true if valid, false otherwise.
     */
    public boolean validatePermissions(final String permissions) {
        return validation.matcher(permissions).matches();
    }

    /**
     * Primary method for doing (once) prep before making calls to do the actual checks.
     * @param userName the owning user of the file/directory/entry
     * @param groupName the owning group of the file/directory/entry
     * @param permissions the permissions stored with the file/directory/entry
     * @param modes requested access modes
     * @return an empty array if all checks pass, otherwise the {@code AccessMode}s that failed.
     */
    private AccessMode[] checkAccessWork(final String userName,
                                         final String groupName,
                                         final String permissions,
                                         final AccessMode... modes) {
        //  No access modes requested, so no need to check.
        if (modes.length == 0) return modes;

        //  Attempt to get the authenticated user.
        Authentication authentication = determineAuthentication();

        //  Admin has access to everything, not allowed to restrict.
        if (NAME_ADMIN_USER.equals(authentication.getName())) {
            return EMPTY_ACCESS_MODES;
        }

        //  Extract the owner permissions to check if the current user is owner of file/directory/entry.
        char[] ownerPermissions = null;
        if (authentication.getName().equals(userName)) {
            ownerPermissions = permissions.substring(0, 3).toCharArray();
        }

        //  Extract the group permission to check when the authenticated user has an "authority" that matches
        //  the owning group of the file/directory/entry.
        char[] groupPermissions = null;
        var match = authentication.getAuthorities()
            .stream()
            .filter(a -> a.getAuthority().equals(groupName))
            .findFirst();
        if (match.isPresent()) {
            groupPermissions   = permissions.substring(3, 6).toCharArray();   
        }

        //  Always extract everyone/world permissions.
        char[] otherPermissions = permissions.substring(6).toCharArray();

        //  We can reduce/simplify work required when only single mode needs to be checked.
        if (modes.length == 1) {
            return checkAccessSingleMode(ownerPermissions, groupPermissions, otherPermissions, modes[0]) ? modes : EMPTY_ACCESS_MODES;
        }
        return checkAccess(ownerPermissions, groupPermissions, otherPermissions, modes);
    }

    /**
     * Check access for each {@code AccessMode} provided against the owner, group, everyone permissions.
     * @param ownerPermissions 3-character permissions for the owning user
     * @param groupPermissions 3-character permissions for the owning group
     * @param otherPermissions 3-character permissions for everyone else
     * @param modes the {@code AccessMode}s which need to be checked
     * @return the {@code AccessMode}s that failed the check.
     */
    private AccessMode[] checkAccess(final char[] ownerPermissions, 
                                     final char[] groupPermissions, 
                                     final char[] otherPermissions,
                                     final AccessMode... modes) {
        //  Check access for all modes and create array to return for the unsuccessful modes (e.g., modes that
        //  didn't pass any of the permission checks).
        return Stream.of(modes)
            .filter(mode -> !checkAccessSingleMode(ownerPermissions, groupPermissions, otherPermissions, mode))
            .sorted()
            .toArray(AccessMode[]::new);
    }

    /**
     * Check access for a single {@code AccessMode} against the owner, group, everyone permissions.
     * @param ownerPermissions 3-character permissions for the owning user
     * @param groupPermissions 3-character permissions for the owning group
     * @param everyonePermissions 3-character permissions for everyone else
     * @param mode the {@code AccessMode} to check
     * @return true is {@code AccessMode} is covered by permissions; false otherwise.
     */
    private boolean checkAccessSingleMode(final char[] ownerPermissions,
                                          final char[] groupPermissions,
                                          final char[] everyonePermissions,
                                          final AccessMode mode) {
        return
            checkAccessUserClass(ownerPermissions, mode) ||
            checkAccessUserClass(groupPermissions, mode) ||
            checkAccessUserClass(everyonePermissions, mode);
    }
    
    /**
     * Compare the access modes requested against the permissions provided.  The permissions are the specific subset
     * being checked: first 3 characters for owning user, second 3 characters for owning group, third 3 characters for
     * everyone else.  Basically implementing Posix security flags for each entry/file/directory.
     * @param permsSubset 3-character permission flags for a specific class of user (owner, group, everyone)
     * @param mode specific {@code AccessMode}s for which to check access
     * @return true if specific mode is available; false otherwise.
     */
    private boolean checkAccessUserClass(final char[] permsSubset, final AccessMode mode) {
        //  Simple case: no permissions provided.
        if (permsSubset == null) return false;

        //  For each AccessMode checked, ensure that the correct Posix-like flag is enabled.
        switch (mode) {
            case READ:
                if (permsSubset[0] != NEO4JFS_PERMISSION_READ) return false;
                break;
            case WRITE:
                if (permsSubset[1] != NEO4JFS_PERMISSION_WRITE) return false;
                break;
            case EXECUTE:
                if (permsSubset[2] != NEO4JFS_PERMISSION_EXECUTE) return false;
                break;
        }

        return true;
    }

    /**
     * Extract authentication from Spring security context
     * @return current user authentication or default, unauthenticated user when no current user found
     */
    private Authentication determineAuthentication() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return SecurityContextHolder.getContext().getAuthentication();
        } else {
            return unknownUnauthenticatedUser;
        }
    }
}
