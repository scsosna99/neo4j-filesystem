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
package dev.scottsosna.neo4jfs.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;

/**
 * Interface for managing Neo4Jfs file system.
 */
public interface FileSystemService {
    /**
     * Creates new or validates existing file system, ensures root "directory" (node) exists and that
     * storage partition is available/usable.
     * @param fsUri Neo4Jfs URI for the file system to initialize.
     */
    void init(final URI fsUri) throws IOException;

    /**
     * Deletes the complete file system, including content managed by Storage Manager.
     * @param fsUri Neo4Jfs URI for the file system to delete.
     */
    void drop(final URI fsUri) throws IOException;

    /**
     * The file system's {@code FileStore} instance is based on Storage Manager's partition
     * @param fsUri Neo4Jfs URI for the file system.
     * @return {@code FileStore} for the partition.
     * @throws IOException if an I/O error occurs.
     */
    FileStore getFileStore(final URI fsUri) throws IOException;
}