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
package dev.scottsosna.neo4jfs.web.controller;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.util.Map;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.NEO4J_URI_TEMPLATE;

/**
 * Base class for application's controllers
 */
public class Neo4jfsController {

    /**
     * Constructor.  Only available to child classes.
     */
    protected Neo4jfsController() {}

    /**
     * Create the base URI for a partition
     * @param partitionId identifies specific partition for the Neo4Jfs file system     * @return fully-qualified URI for the partition
     */
    protected URI uri(final String partitionId) {
        return URI.create(NEO4J_URI_TEMPLATE.formatted(partitionId));
    }

    /**
     * Check whether Neo4Jfs file system for this partition exists, if not create it.
     * @param partitionId identifies specific partition for the Neo4Jfs file system
     * @return file system for the partition
     * @throws IOException when file system cannot be created for whatever reason
     */
    protected FileSystem getFileSystem(final String partitionId) throws IOException {
        URI fsUri = uri(partitionId);
        try {
            return FileSystems.getFileSystem(fsUri);
        } catch (FileSystemNotFoundException e) {
            return FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of());
        }
    }
}
