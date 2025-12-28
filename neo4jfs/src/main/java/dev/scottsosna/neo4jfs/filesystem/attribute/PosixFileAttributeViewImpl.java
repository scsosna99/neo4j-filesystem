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

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Neo4Jfs implementation of PosixFileAttributeView.
 */
public class PosixFileAttributeViewImpl extends FileOwnerAttributeViewImpl implements PosixFileAttributeView {

    public static final String VIEW_NAME = "posix";

    /**
     * Constructor
     * @param entry file/directory providing the attributes.
     */
    public PosixFileAttributeViewImpl(BaseEntry entry) {
        super(entry);
    }

    /**
     * Returns the name of the attribute view. Attribute views of this type have the name "owner".
     * @return name of the attribute view
     */
    @Override
    public String name() {
        return VIEW_NAME;
    }

    /**
     * Reads the basic file attributes as a bulk operation.
     *
     * @return the file attributes
     * @throws IOException is an I/O error occurs
     */
    @Override
    public PosixFileAttributes readAttributes() throws IOException {
        return new PosixFileAttributesImpl(entry);
    }

    /**
     * Set permissions on a file/directory overriding either exisitng or inherited permissions.
     * @param perms the new set of permissions
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
        entry.setPosixPermissions(perms);
        persist();
    }

    /**
     * Update file/directory group owner.
     * @param group the new file/group group-owner
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void setGroup(GroupPrincipal group) throws IOException {
        entry.setGroupName( group.getName());
        persist();
    }
}
