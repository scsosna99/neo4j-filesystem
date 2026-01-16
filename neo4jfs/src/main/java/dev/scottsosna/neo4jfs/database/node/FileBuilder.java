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

/**
 * {@code FileEntry} builder
 */
public class FileBuilder {

    boolean hidden = false;
    private String name = null;
    private String groupName = null;
    private String userName = null;
    private String permissions = null;
    private String storageId = null;
    private Long size;

    /**
     * Default, no-args constructor
     */
    public FileBuilder () {
        //  need default constructor
    }

    /**
     * Constructor
     * @param parent directory in which new file will be created.
     */
    public FileBuilder(final DirectoryEntry parent) {
        //  Certain values inherited from parent unless overridden.
        this.userName = parent.ownerUserName;
        this.groupName = parent.ownerGroupName;
    }

    /**
     * Creates new {@code FileEntry} node and sets the properties as appropriate.
     * @return {@code FileEntry} instance with properties set by builder
     */
    public FileEntry build() {
        FileEntry file = new FileEntry();
        file.name = name;
        file.ownerUserName = userName;
        file.ownerGroupName = groupName;
        file.permissions = permissions;
        file.hidden = hidden;
        file.storageId = storageId;
        file.size = size;
        file.hidden = false;
        return file;
    }

    /** -----------------------------------------------------------------------
     * Setters for mutable properties.
     * ------------------------------------------------------------------------ */

    public FileBuilder setGroupName(final String groupName) {
        this.groupName = groupName;
        return this;
    }

    public FileBuilder setName(final String name) {
        this.name = name;
        return this;
    }

    public FileBuilder setStorageId(final String storageId) {
        this.storageId = storageId;
        return this;
    }

    public FileBuilder setSize(final Long size) {
        this.size = size;
        return this;
    }

    public FileBuilder setUserName(final String userName) {
        this.userName = userName;
        return this;
    }
}
