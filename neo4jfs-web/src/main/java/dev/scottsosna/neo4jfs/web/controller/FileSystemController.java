package dev.scottsosna.neo4jfs.web.controller;

import dev.scottsosna.neo4jfs.service.FileSystemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * API endpoints for managing Neo4J file system.
 */
@RestController
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
