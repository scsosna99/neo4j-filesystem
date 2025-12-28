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
package dev.scottsosna.neo4jfs.database.repository.util;

import dev.scottsosna.neo4jfs.security.AccessManager;
import dev.scottsosna.neo4jfs.util.SpringContext;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for transforming the {@code PosixFilePermission} enum used by Java NIO into a *nix-like permissions
 * string (e.g., output from ls -l command) and vice-versa.
 */
public class PosixFilePermissionConverter {

    private static AccessManager accessManager;

    /**
     * Neo4Jfs permissions are stored as the 9-character string displayed by a Unix "ls -l" command.  This static
     * pattern can be used to validate that the permissions string is correctly formatted to prevent errors.
     */
    private static Pattern validation = Pattern.compile("^[r-][w-][x-][r-][w-][x-][r-][w-][x-]$");

    /**
     * Convert set of {@link PosixFilePermission} to a (somewhat) human-readable string as displayed by *nix ls -l command
     * @param posixPermissions Posix permissions to convert
     * @return String representation of permissions stored with entry.
     */
    public static String convert(final Set<PosixFilePermission> posixPermissions) {
        return getAccessManager().convertPermissions(posixPermissions);
    }

    /**
     * Convert human-readable string to set of {@link PosixFilePermission}
     * @param permissions permission string, most likely for a file or directory.
     * @return Corresponding set of permissions.
     */
    public static Set<PosixFilePermission> convert(final String permissions) {
        return getAccessManager().convertPermissions(permissions);
    }

    /**
     * @return current {@code AccessManager} implementation.
     */
    private static AccessManager getAccessManager() {
        if (accessManager == null) {
            accessManager = SpringContext.getBean(AccessManager.class);
        }

        return accessManager;
    }
}
