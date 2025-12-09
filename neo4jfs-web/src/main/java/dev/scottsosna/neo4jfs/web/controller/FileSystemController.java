package dev.scottsosna.neo4jfs.web.controller;

import dev.scottsosna.neo4jfs.service.FileSystemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

/**
 * API endpoints for managing Neo4J file system.
 */
@RestController
@RequestMapping( value = "/neo4jfs/api/filesystem")
public class FileSystemController {

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
     * @param fsUri base Neo4Jfs URI for the file system.
     * @throws IOException if I/O error occurs during initialization.
     */
    @PostMapping("initialize")
    @ResponseStatus(HttpStatus.CREATED)
    public void initialize(@RequestBody final URI fsUri) throws IOException {
        service.init(fsUri);
    }

    /**
     * Drops file system.  NOTE: DESTRUCTIVE, completely destroys all Neo4J data and underlying files
     * @param fsUri base Neo4Jfs URI for the file system.
     */
    @PostMapping("drop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void drop(@RequestBody final URI fsUri) {
        service.drop(fsUri);
    }
}
