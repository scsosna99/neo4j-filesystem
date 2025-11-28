package dev.scottsosna.sandbox.neo4jfs.service;

import dev.scottsosna.sandbox.neo4jfs.database.model.neo4j.Database;
import dev.scottsosna.sandbox.neo4jfs.database.model.neo4j.DatabaseAccessType;
import dev.scottsosna.sandbox.neo4jfs.database.model.neo4j.DatabaseStatusType;
import dev.scottsosna.sandbox.neo4jfs.database.model.neo4j.DatabaseType;
import dev.scottsosna.sandbox.neo4jfs.database.repository.DatabaseRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class FileSystemServiceImpl extends BaseNeo4jfsService implements FileSystemService {

    private final DatabaseRepository repository;
    private final DirectoryService directoryService;
    private final FileService fileService;
    private final StorageManager storageManager;

    /**
     * Constructor
     */
    public FileSystemServiceImpl(DatabaseRepository repository,
                                 DirectoryService directoryService,
                                 FileService fileService,
                                 StorageManager storageManager) {
        this.repository = repository;
        this.directoryService = directoryService;
        this.fileService = fileService;
        this.storageManager = storageManager;
    }

    /**
     * Creates new or validates existing file system, ensures root "directory" (node) exists and that
     * storage partition is available/usable.
     * @param uri base URI for the file system.
     */
    public void init(URI uri) {
        checkSchema(uri);

        //  Does a database exist for the partition?
        Database db = repository.find(uri);
        if (db != null) {
            //  Database exists, verify usability
            verifyDatabaseUsability(db);
            directoryService.findOrCreateRoot(uri);
        } else {
            //  No existing database, create new and add root directory.
            repository.create(uri);
            directoryService.createRoot(uri);
        }

        try {
            //  Also make sure storage manager is available/initialized.
            storageManager.initPartition(uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Completely deletes the Neo4J file system.
     * @param uri base URI for the file system.
     */
    public void drop(URI uri) {
        //  Best effort made.  If Neo4J database fails to delete, no attempt to delete storage partition so the file
        //  system is still usable.  If Neo4J dataabase is deleted but storage partition fails to delete, the
        //  physical files are left dangling: unfortunate, but since Neo4J database is gone files aren't usable.
        //  Manual cleanup at a later date.
        try {
            repository.drop(uri);
            storageManager.dropPartition(uri);
        } catch (Exception e) {

        }
    }

    /**
     * The URI has specified an existing database, ensure that the database meets the criteria for a Neo4J file system.
     * @param db Database to verify.
     */
    private void verifyDatabaseUsability(Database db) {
        if (db.getDefaultDatabase()) throw new RuntimeException("Default databases are not allowed.");
        if (db.getType() == DatabaseType.SYSTEM) throw new RuntimeException("System databases are not allowed.");
        if (db.getAccess() != DatabaseAccessType.READ_WRITE) throw new RuntimeException("Database must be read-write.");
        if (db.getCurrentStatus() != DatabaseStatusType.ONLINE) throw new RuntimeException("Database must be online.");
    }

    @PostConstruct
    public void test() {
        try {
            try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://scsosna123/"),
                Map.of(FileSystemService.class.getName(), this, DirectoryService.class.getName(), directoryService))) {
                Path path = Path.of(new URI("neo4jfs://scsosna123/abc/def/ghi"));
                Files.delete(path);
            }

            drop(URI.create("neo4jfs://scsosna123/"));
            drop(URI.create("neo4jfs://scsosna345/"));

            init(new URI("neo4jfs://scsosna99/"));
            fileService.create(new URI("neo4jfs://scsosna99/myRootFile"), new File("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));
            directoryService.mkdir(new URI("neo4jfs://scsosna99/abc"));
            fileService.create(new URI("neo4jfs://scsosna99/abc/myFirstFile"), new File("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));
            directoryService.mkdir(new URI("neo4jfs://scsosna99/def"));
            directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij"));
            directoryService.mkdir(new URI("neo4jfs://scsosna99/def/klm"));
            fileService.create(new URI("neo4jfs://scsosna99/def/mySecondFile"), new File("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));
            fileService.create(new URI("neo4jfs://scsosna99/def/myThirdFile"), new File("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));
            System.out.println("completed");
            directoryService.dumpTree(new URI("neo4jfs://scsosna99/"));
            drop(new URI("neo4jfs://scsosna99/"));


//            directoryService.mkdir(new URI("neo4jfs://scsosna98/abc/def"));
//            directoryService.mkdir(new URI("neo4jfs://scsosna98/abc/yui"));
//            directoryService.mkdir(new URI("neo4jfs://scsosna98/abc/def/qqq"));
//            var p = directoryService.parent(new URI("neo4jfs://scsosna98/abc/def/qqq"));
//            System.out.println(p);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
