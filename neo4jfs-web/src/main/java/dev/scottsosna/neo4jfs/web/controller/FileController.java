package dev.scottsosna.neo4jfs.web.controller;

import dev.scottsosna.neo4jfs.service.FileService;
import dev.scottsosna.neo4jfs.service.FileSystemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

/**
 * API endpoints for managing Neo4Jfs files.
 */
@RestController
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
