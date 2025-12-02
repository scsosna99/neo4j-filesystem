package dev.scottsosna.neo4jfs.controller;

import dev.scottsosna.neo4jfs.service.FileSystemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping( value = "neo4jfs/api/filesystem")
public class FileSystemController {

    private FileSystemService service;

    public FileSystemController(FileSystemService service) {
        this.service = service;
    }

    @PostMapping("initialize")
    @ResponseStatus(HttpStatus.CREATED)
    public void initialize(@RequestBody URI uri) throws IOException {
        service.init(uri);
    }

    @PostMapping("drop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void drop(@RequestBody URI uri) {
        service.drop(uri);
    }
}
