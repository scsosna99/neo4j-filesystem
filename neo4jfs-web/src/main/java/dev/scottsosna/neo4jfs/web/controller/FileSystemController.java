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

import dev.scottsosna.neo4jfs.service.FileSystemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;

/**
 * API endpoints for managing Neo4J file system.
 */
@RestController
@SecurityRequirement(name = "basicAuth")
@PreAuthorize("isAuthenticated()")
@RequestMapping( value = "/neo4jfs/api/filesystem/{partitionId}")
public class FileSystemController extends Neo4jfsController {

    /**
     * Service for managing overall file system.
     */
    private FileSystemService service;

    /**
     * Constructor
     * @param service file system service
     */
    public FileSystemController(final FileSystemService service) {
        this.service = service;
    }

    /**
     * Initializes file system, either creating new or opening existing.
     * @throws IOException if I/O error occurs during initialization.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void initialize(@PathVariable("partitionId") final String partitionId) throws IOException {
        service.init(uri(partitionId));
    }

    /**
     * Drops file system.  NOTE: DESTRUCTIVE, completely destroys all Neo4J data and underlying files
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void drop(@PathVariable("partitionId") final String partitionId) throws IOException {
        service.drop(uri(partitionId));
    }
}
