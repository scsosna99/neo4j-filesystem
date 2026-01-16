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

import java.nio.file.AccessMode;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public interface AccessManager {
    /**
     * Checks access of entry against the AccessModes provided.
     * @param entry the file/directory/entry to check permissions
     * @param modes requested access modes
     * @return an empty array if all checks pass, otherwise the {@code AccessMode}s that failed.
     */
    AccessMode[] checkAccess(final BaseEntry entry, final AccessMode... modes);

    /**
     * Convert set of {@link PosixFilePermission} to the form required by {@code AccessManager} implementation.
     * @param posixPermissions Posix permissions to convert
     * @return String representation of permissions stored with entry.
     */
    String convertPermissions(final Set<PosixFilePermission> posixPermissions);

    /**
     * Convert human-readable string to set of {@link PosixFilePermission}
     * @param permissions permission string, most likely for a file/directory/entry.
     * @return corresponding set of Posix permissions.
     */
    Set<PosixFilePermission> convertPermissions(final String permissions);

    /**
     * @return the admin group for the AccessManager implementation.
     */
    String getAdminGroup();

    /**
     * @return the admin user for the AccessManager implementation.
     */
    String getAdminUser();

    /**
     * Is currently-authenticated user considered admin or super-user?
     * @return true if admin; false otherwise
     */
    boolean isAdminUser();

    /**
     * @return root directory permissions used when creating file system.
     */
    String rootPermissions();

    /**
     * @return name of current authenticaated user
     */
    String userName();

    /**
     * Validate the permission string against the {@code AccessManager} expected form.
     * @param permissions the permissions string to be validated
     * @return true if valid, false otherwise.
     */
    boolean validatePermissions(final String permissions);
}
