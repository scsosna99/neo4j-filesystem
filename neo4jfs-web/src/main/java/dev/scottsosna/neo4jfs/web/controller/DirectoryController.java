/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.web.controller;

import dev.scottsosna.neo4jfs.filesystem.attribute.UserPrincipalImpl;
import dev.scottsosna.neo4jfs.filesystem.option.Neo4jfsCopyOption;
import dev.scottsosna.neo4jfs.service.DirectoryService;
import dev.scottsosna.neo4jfs.web.controller.request.ChangeOwnerRequest;
import dev.scottsosna.neo4jfs.web.controller.request.SinglePathRequest;
import dev.scottsosna.neo4jfs.web.controller.request.SourceDestinationPathRequest;
import dev.scottsosna.neo4jfs.web.controller.request.TempDirectoryRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

/**
 * API endpoints for managing Neo4Jfs directories.
 */
@RestController
@SecurityRequirement(name = "basicAuth")
@PreAuthorize("isAuthenticated()")
@RequestMapping( value = "/neo4jfs/api/directory/{partition}")
public class DirectoryController extends Neo4jfsController {

    /**
     * Service for managing directories.
     */
    DirectoryService service;

    /**
     * Constructor
     * @param service directory service
     */
    public DirectoryController(final DirectoryService service) {
        this.service = service;
    }

    /**
     * Create a directory
     * @param partition Neo4Jfs partition identifier
     * @param intermediates query param, should intermediate directories be created if they don't exist?
     * @param request request which contains the path to create
     * @return http status 201 if successful, http status 500 otherwise
     */
    @PostMapping()
    public ResponseEntity<?> create(@PathVariable("partition") final String partition,
                                    @RequestParam(required = false, defaultValue = "false") final boolean intermediates,
                                    @RequestBody final SinglePathRequest request) {

        try {
            //  Get the file system for the partition.
            FileSystem fs = getFileSystem(partition);

            //  Create the directory in one of two ways
            Path path = fs.getPath(request.path());
            if (intermediates) {
                Files.createDirectories(path);
            } else {
                Files.createDirectory(path);
            }

            //  Successful
            return ResponseEntity.created(path.toUri()).build();
        } catch (IOException e) {
            //  Error occurred
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create temporary directory
     * @param partition Neo4Jfs partition identifier
     * @param request request containing directory and temp suffix to apply
     * @return http status 201 if successful, http status 500 otherwise
     */
    @PostMapping("temp")
    public ResponseEntity<?> createTemp(@PathVariable("partition") final String partition,
                                        @RequestBody final TempDirectoryRequest request) {

        try {
            //  Get the file system for the partition.
            FileSystem fs = getFileSystem(partition);

            //  Create the temporary directory
            Path path = fs.getPath(request.path());
            Files.createTempDirectory(path, request.prefix());

            //  Successful
            return ResponseEntity.created(path.toUri()).build();
        } catch (IOException e) {
            //  Error occurred
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a directory
     * @param partition Neo4Jfs partition identifier
     * @param request request containing path of directory to delete
     * @return http status 204 if successful, http status 500 otherwise
     */
    @DeleteMapping
    public ResponseEntity<?> delete(@PathVariable("partition") final String partition,
                                    @RequestBody final SinglePathRequest request) {
        try {
            // Get the file system for the partition.
            FileSystem fs = getFileSystem(partition);

            // Confirm directory exists and is a directory
            Path path = fs.getPath(request.path());
            if (Files.isDirectory(path)) {
                // Delete directory, success!
                Files.delete(path);
                return ResponseEntity.noContent().build();
            } else {
                throw new NotDirectoryException(path.toString());
            }
        } catch (IOException e) {
            // Error occurred
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Copy a file or directory
     * @param partition Neo4Jfs partition identifier
     * @param request request contains source/destination paths being copied
     * @return http status 200 if successful, http status 500 otherwise
     */
    @PostMapping("/copy")
    public ResponseEntity<?> copy(@PathVariable("partition") final String partition,
                                  @RequestBody final SourceDestinationPathRequest request) {
        try {
            //  Get the file system for the partition.
            FileSystem fs = getFileSystem(partition);

            //  Copy the file or directory.
            Files.copy(fs.getPath(request.sourcePath()), fs.getPath(request.destinationPath()), Neo4jfsCopyOption.COPY_RECURSIVELY);

            //  Success
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            //  Error occurred
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * MOve a file or directory
     * @param partition Neo4Jfs partition identifier
     * @param request contains path of source and destination
     * @return http status 200 if successful, http status 500 otherwise
     */
    @PostMapping("/move")
    public ResponseEntity<?> move(@PathVariable("partition") final String partition,
                                  @RequestBody final SourceDestinationPathRequest request) {
        try {
            // Get the file system for the partition.
            FileSystem fs = getFileSystem(partition);

            //  Move the file or directory
            Files.move(fs.getPath(request.sourcePath()), fs.getPath(request.destinationPath()));

            // Success
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            // Error occurred
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check whether the directory exists.
     * @param partition Neo4Jfs partition identifier
     * @param request request contains the path of interest
     * @return http status 200 if directory exists, http status 404 if it doesn't exist or is not a directory, 500 otherwise
     */
    @PostMapping("/exists")
    public ResponseEntity<?> exists(@PathVariable("partition") final String partition,
                                    @RequestBody final SinglePathRequest request) {
        try {
            // Get the file system for the partition
            FileSystem fs = getFileSystem(partition);

            // Check whether path exists and it is a directory.
            Path path = fs.getPath(request.path());
            if (Files.exists(path) && Files.isDirectory(path)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Change the owner of a file or directory
     * @param partition Neo4Jfs partition identifier
     * @param request request containing file/directory path and its new owner.
     * @return
     */
    @PutMapping("/owner")
    public ResponseEntity<?> changeOwner(@PathVariable("partition") final String partition,
                                         @RequestBody final ChangeOwnerRequest request) {
        try {
            // Get the file system for the partition
            FileSystem fs = getFileSystem(partition);

            //  Set the new owher for the file or directory
            Files.setOwner(fs.getPath(request.path()), new UserPrincipalImpl(request.owner()));

            //  Success
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            // Error occurred
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
