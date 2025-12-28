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
package dev.scottsosna.neo4jfs.filesystem.attribute;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

/**
 * Simple implementation of PosixFileAttributes.
 */
public class PosixFileAttributesImpl extends BasicFileAttributesImpl implements PosixFileAttributes {

    /**
     * Constructor
     * @param entry file/directory providing the attributes.
     */
    public PosixFileAttributesImpl(BaseEntry entry) {
        super(entry);
    }

    /**
     * @return owner of the file/directory
     */
    @Override
    public UserPrincipal owner() {
        return new UserPrincipalImpl(entry.getOwnerUserName());
    }

    /**
     * @return owning group of the file/directory
     */
    @Override
    public GroupPrincipal group() {
        return new GroupPrincipalImpl(entry.getOwnerGroupName());
    }

    /**
     * Permissions for this file/directory as a set of PosixFilePermissions.
     * @return
     */
    @Override
    public Set<PosixFilePermission> permissions() {
        return entry.getPosixPermissions();
    }
}
