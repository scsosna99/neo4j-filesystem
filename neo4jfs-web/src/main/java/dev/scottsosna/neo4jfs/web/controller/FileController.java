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

import dev.scottsosna.neo4jfs.service.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.net.URI;

/**
 * API endpoints for managing Neo4Jfs files.
 */
@RestController
@SecurityRequirement(name = "basicAuth")
@PreAuthorize("isAuthenticated()")
@RequestMapping( value = "/neo4jfs/api/file/{partitionId}")
public class FileController extends Neo4jfsController {

    /**
     * Service for managing files.
     */
    private FileService service;

    /**
     * Constructor
     * @param service file service
     */
    public FileController(final FileService service) {
        this.service = service;
    }

    /**
     * Initializes file system, either creating new or opening existing.
     * @param fsUri base Neo4Jfs URI for the file system.
     * @throws IOException if I/O error occurs during initialization.
     */
    @PostMapping("initialize")
    @ResponseStatus(HttpStatus.CREATED)
    public void initialize(@RequestBody final URI fsUri) throws IOException {
    }
}
