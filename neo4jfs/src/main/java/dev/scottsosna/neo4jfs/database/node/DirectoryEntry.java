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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

/**
 * Neo4J node representing a directory.
 */
@NodeEntity( label = "Directory")
@Getter @Setter @NoArgsConstructor
public class DirectoryEntry extends BaseEntry {

    /**
     * Flag: is the directory the root?
     */
    boolean root;

    /**
     * Subdirectories of this directory as defined by PARENT_OF relationship.
     */
    @Relationship(type = "PARENT_OF")
    List<DirectoryEntry> subdirs;

    /**
     * Files contained within this directory.
     */
    @Relationship(type = "CONTAINS")
    List<FileEntry> files;
}
