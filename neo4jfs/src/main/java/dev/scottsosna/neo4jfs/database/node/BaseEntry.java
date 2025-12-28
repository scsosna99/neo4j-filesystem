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
package dev.scottsosna.neo4jfs.database.node;

import dev.scottsosna.neo4jfs.database.repository.util.PosixFilePermissionConverter;
import lombok.*;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.id.UuidStrategy;

import java.net.URI;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Set;

/**
 * Basic information required of all Neo4Jfs entries (files, directories, etc.)
 */
@Getter @Setter @NoArgsConstructor @EqualsAndHashCode
public class BaseEntry {

    /**
     * Neo4J node identifier, auto-generated UUID.
     */
    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    String id;

    /**
     * Name of the entry
     */
    String name;

    /**
     * Username of creator, owner of the entry.
     */
    String userName;

    /**
     * Name of group associated with this entry, usually for security reasons.
     */
    String groupName;

    /**
     * Posix permissions in their typical representation, e.g., "rwxr-xr-x".
     */
    @Setter(AccessLevel.NONE)
    String permissions;

    /**
     * Date/time when entity was created.
     */
    Instant created;

    /**
     * Date/time when entity was last modified.
     */
    Instant lastModified;

    /**
     * Date/time when entity was last accessed.
     */
    Instant lastAccessed;

    /**
     * Flag: is entry considered "hidden" when listing/walking directory contents?
     */
    boolean hidden;


    @Transient
    @Setter(AccessLevel.NONE)
    String inheritedPermissions;

    /**
     * In which Neo4Jfs file system is this entry located?
     */
    @Transient
    URI fsUri;

    /**
     * Permissions stored with enty is converted into a set of PosixFilePermissions.
     * @return set of PosixFilePermissions
     */
    public Set<PosixFilePermission> getPosixPermissions() {
        return PosixFilePermissionConverter.convert(permissions != null ? permissions : inheritedPermissions);
    }

    /**
     * Take a set of permissions and convert to its string representation.
     * @param posixPermissions Set of Posix file permissions converted into a string representation.
     */
    public void setPosixPermissions(final Set<PosixFilePermission> posixPermissions) {
        permissions = PosixFilePermissionConverter.convert(posixPermissions);
    }

    /**
     * Inherited permissions are derived when the file/directory itself has no permissions defined on it, meaning
     * that the containing directory - the parent of the file or directory - provides its permissions (which themselves
     * may be inherited).
     * @param parent the parent directory of this entry (file or directory).
     */
    public void deriveInheritedPermissions(final DirectoryEntry parent) {
        if (permissions == null && parent != null) {
            if (parent.getInheritedPermissions() != null) {
                inheritedPermissions = parent.getInheritedPermissions();
            } else {
                inheritedPermissions = parent.getPermissions();
            }
        }
    }
}
