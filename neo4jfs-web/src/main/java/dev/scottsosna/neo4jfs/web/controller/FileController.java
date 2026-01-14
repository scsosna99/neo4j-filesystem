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
import dev.scottsosna.neo4jfs.web.controller.request.SinglePathRequest;
import dev.scottsosna.neo4jfs.web.controller.request.TempFileRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * API endpoints for managing Neo4Jfs files.
 */
@RestController
@SecurityRequirement(name = "basicAuth")
@PreAuthorize("isAuthenticated()")
@RequestMapping( value = "/neo4jfs/api/file/{partition}")
public class FileController extends Neo4jfsController {

    /**
     * Service for managing files.
     */
    private final FileService service;

    /**
     * Constructor
     * @param service file service
     */
    public FileController(final FileService service) {
        this.service = service;
    }

    /**
     * Download a file from Neo4Jfs
     * @param partition Neo4Jfs partition identifier
     * @param request file to download
     * @return http status 200 if successful, 404 if path doesn't exists or is not a file, 500 otherwise
     */
    @PostMapping("download")
    public ResponseEntity<Resource> download(
        @PathVariable("partition") final String partition,
        @RequestBody final SinglePathRequest request) {

        try {
            // Ensure filesystem exists for the partition
            FileSystem fs = getFileSystem(partition);

            //  Confirm that path exists and it is a file.
            Path fullPath = fs.getPath(request.path());
            if (!Files.exists(fullPath) || !Files.isRegularFile(fullPath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            //  Attempt to get input stream for file which is wrapped by the resource returned to API caller
            InputStreamResource resource = new InputStreamResource(service.getInputStream(fullPath.toUri()));

            //  ResponseEntity contains resouurce for caller to get data via stream
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + fullPath.getFileName() + "\"")
                .contentLength(Files.size(fullPath))
                .body(resource);
        } catch (IOException e) {
            //  Error occurred
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new Neo4Jfs file and upload content.
     * @param partition Neo4Jfs partition name
     * @param file the file to upload
     * @param path destination for uploaded file.
     * @return 201 when file successfully created, 500 for any other error.
     */
    @PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @PathVariable("partition") final String partition,
            @RequestPart("file") final MultipartFile file,
            @RequestPart("path") final String path) {

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("file part is required and must not be empty");
            }

            // Ensure filesystem exists for the partition
            FileSystem fs = getFileSystem(partition);

            //  Copy the file into Neo4Jfs
            Path fullPath = fs.getPath(path);
            Files.copy(file.getInputStream(), fullPath);

            return ResponseEntity.created(fullPath.toUri()).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Creates a new, empty file.
     * @param partition Neo4Jfs partition identifier
     * @param request request containing path for new file
     * @return http status 201 if successful, http status 500 otherwise
     */
    @PostMapping()
    public ResponseEntity<?> create(
        @PathVariable("partition") final String partition,
        @RequestBody final SinglePathRequest request) {

        try {
            // Ensure filesystem exists for the partition
            FileSystem fs = getFileSystem(partition);

            //  Create new empty file in Neo4Jfs
            Path fullPath = fs.getPath(request.path());
            Files.createFile(fullPath);

            //  Success
            return ResponseEntity.created(fullPath.toUri()).build();
        } catch (IOException e) {
            //  Error occurred
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new temporary file.
     * @param partition Neo4Jfs partition identifier
     * @param request contains directory into which temp file is created plus temp file's prefix/suffix
     * @return http status 201 if successful, http status 500 otherwise
     */
    @PostMapping("temp")
    public ResponseEntity<?> createTemp(
        @PathVariable("partition") final String partition,
        @RequestBody final TempFileRequest request) {

        try {
            // Ensure filesystem exists for the partition
            FileSystem fs = getFileSystem(partition);

            //  Create new empty file in Neo4Jfs
            Path fullPath = fs.getPath(request.path());
            Files.createTempFile(fullPath, request.prefix(), request.suffix());

            //  Success
            return ResponseEntity.created(fullPath.toUri()).build();
        } catch (IOException e) {
            //  Error occurred
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a file from Neo4Jfs.
     * @param partition Neo4Jfs partition identifier
     * @param request pathname of file to delete in request
     * @return http status 204 if success, http status 404 if path not found or is not a file, http status 500 otherwise
     */
    @DeleteMapping()
    public ResponseEntity<?> delete(
        @PathVariable("partition") final String partition,
        @RequestBody final SinglePathRequest request) {

        try {
            // Get the file system for the partition
            FileSystem fs = getFileSystem(partition);

            //  Create new empty file in Neo4Jfs
            Path fullPath = fs.getPath(request.path());
            if (Files.exists(fullPath) && Files.isRegularFile(fullPath)) {
                Files.delete(fullPath);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check whether the file exists.
     * @param partition Neo4Jfs partition identifier
     * @param request request contains the path of interest
     * @return http status 200 if files exists, http status 404 if it doesn't exist or is not a file, 500 otherwise
     */
    @PostMapping("/exists")
    public ResponseEntity<?> exists(@PathVariable("partition") final String partition,
                                    @RequestBody final SinglePathRequest request) {
        try {
            // Get the file system for the partition
            FileSystem fs = getFileSystem(partition);

            // Check whether path exists and it is a file.
            Path path = fs.getPath(request.path());
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
