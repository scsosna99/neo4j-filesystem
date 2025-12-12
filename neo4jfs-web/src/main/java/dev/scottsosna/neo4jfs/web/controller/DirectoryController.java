package dev.scottsosna.neo4jfs.web.controller;

import dev.scottsosna.neo4jfs.service.DirectoryService;
import dev.scottsosna.neo4jfs.service.FileSystemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

/**
 * API endpoints for managing Neo4Jfs directories.
 */
@RestController
@RequestMapping( value = "/neo4jfs/api/directory/{partitionId}")
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
     * Initializes file system, either creating new or opening existing.
     * @param fsUri base Neo4Jfs URI for the file system.
     * @throws IOException if I/O error occurs during initialization.
     */
    @PostMapping("initialize")
    @ResponseStatus(HttpStatus.CREATED)
    public void initialize(@RequestBody final URI fsUri) throws IOException {
    }
}
