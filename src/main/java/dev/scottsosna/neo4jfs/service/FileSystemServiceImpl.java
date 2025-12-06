package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.model.neo4j.Database;
import dev.scottsosna.neo4jfs.database.model.neo4j.DatabaseAccessType;
import dev.scottsosna.neo4jfs.database.model.neo4j.DatabaseStatusType;
import dev.scottsosna.neo4jfs.database.model.neo4j.DatabaseType;
import dev.scottsosna.neo4jfs.database.repository.DatabaseRepository;
import dev.scottsosna.neo4jfs.database.repository.util.DebuggingFileVisitor;
import dev.scottsosna.neo4jfs.exception.Neo4jfsDatabaseException;
import dev.scottsosna.neo4jfs.storage.StorageManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Map;

@Slf4j
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
     * @param fsUri Neo4Jfs URI for the file system to initialize.
     */
    public void init(URI fsUri) throws IOException {
        checkUri(fsUri);

        //  Does a database exist for the partition?
        Database db = repository.find(fsUri);
        if (db != null) {
            //  Database exists, verify usability
            verifyDatabaseUsability(db);
            directoryService.findOrCreateRoot(fsUri);
        } else {
            //  No existing database, create new and add root directory.
            repository.create(fsUri);
            directoryService.createRoot(fsUri);
        }

        //  Also make sure storage manager is available/initialized.
        storageManager.initPartition(fsUri);
    }

    /**
     * Deletes the complete file system, including content managed by Storage Manager.
     * @param fsUri Neo4Jfs URI for the file system to delete.
     */
    public void drop(URI fsUri) {
        //  Best effort made.  If Neo4J database fails to delete, no attempt to delete storage partition so the file
        //  system is still usable.  If Neo4J dataabase is deleted but storage partition fails to delete, the
        //  physical files are left dangling: unfortunate, but since Neo4J database is gone files aren't usable.
        //  Manual cleanup at a later date.
        try {
            repository.drop(fsUri);
            storageManager.dropPartition(fsUri);
        } catch (Exception e) {

        }
    }

    public FileStore getFileStore(URI uri) throws IOException {
        return storageManager.getPartitionFileStore(uri);
    }

    /**
     * The Neo4fJfs URI partition defined for an existing database which must meet requirements before use.
     * @param db Database to verify.
     */
    private void verifyDatabaseUsability(Database db) throws IOException {
        if (db.getDefaultDatabase()) throw new Neo4jfsDatabaseException("%s: Partition database must not be default.".formatted(db.getName()));
        if (db.getType() == DatabaseType.SYSTEM) throw new Neo4jfsDatabaseException("%s: Partition database must not be system.".formatted(db.getName()));
        if (db.getAccess() != DatabaseAccessType.READ_WRITE) throw new Neo4jfsDatabaseException("%s: Partition database must be read-write.".formatted(db.getName()));
        if (db.getCurrentStatus() != DatabaseStatusType.ONLINE) throw new Neo4jfsDatabaseException("%s: Partition database must be online.".formatted(db.getName()));
    }

    @Scheduled(initialDelay = 2000L)
    public void test() {
        try {
            try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://scsosna99/"), Map.of())) {
//                Files.getFileStore(Path.of(new URI("neo4jfs://scsosna99")));
                Files.createDirectory(fs.getPath("/scs1"));
                Files.createDirectory(fs.getPath("/scs1/scs2"));

                fileService.create(new URI("neo4jfs://scsosna99/myRootFile"), Path.of("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/abc"));
                fileService.create(new URI("neo4jfs://scsosna99/abc/myFirstFile"), Path.of("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/klm"));
                fileService.create(new URI("neo4jfs://scsosna99/def/mySecondFile"), Path.of("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));
                fileService.create(new URI("neo4jfs://scsosna99/def/myThirdFile"), Path.of("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));
                fileService.create(new URI("neo4jfs://scsosna99/def/myFourthFile"), Path.of("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));
                fileService.delete(new URI("neo4jfs://scsosna99/def/myFourthFile"));

                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij/test0"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij/test1"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij/test2"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij/test3"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij/test4"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij/test5"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij/test6"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij/test7"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij/test8"));
                directoryService.mkdir(new URI("neo4jfs://scsosna99/def/hij/test9"));
                fileService.create(new URI("neo4jfs://scsosna99/def/hij/myFirstFile"), Path.of("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));
                fileService.create(new URI("neo4jfs://scsosna99/def/hij/test9//myFirstFile"), Path.of("/Users/scsosna/data/music/manu/viva_la_colifata/1_02_Sabias_Palabras.mp3"));

                Files.newDirectoryStream(fs.getPath("/def/hij")).forEach(System.out::println);
                FileSystemUtils.copyRecursively(fs.getPath("/def"), fs.getPath("/scs1"));


                //                directoryService.dumpTree(new URI("neo4jfs://scsosna99/"));
                Files.copy(Path.of("/Users/scsosna/data/music/manu/siberie_metait_conte/14_Siberie_Fleuve_Amour.mp3"), fs.getPath("/abc/random.mp3"));
                Files.move(fs.getPath("/myRootFile"), fs.getPath("/def/mySecondFile"), StandardCopyOption.REPLACE_EXISTING);
//                directoryService.dumpTree(new URI("neo4jfs://scsosna99/"));
//                Files.walkFileTree(Path.of("/Users/scsosna/data/src/github/neo4jfs/build"), 1, null);
//                FileSystemUtils.copyRecursively(fs.getPath("/def"), fs.getPath("/abc"));
//                Files.move(fs.getPath("/abc"), fs.getPath("/xyz"));
//                Files.move(fs.getPath("/myRootFile"), fs.getPath("/myRootFile"));
//                Files.move(fs.getPath("/myRootFile"), fs.getPath("/myRootFileRenamed"));
//                Files.move(fs.getPath("/xyz"), fs.getPath("/scs1"), StandardCopyOption.REPLACE_EXISTING);
//                Files.move(fs.getPath("/def/hij"), fs.getPath("/def/mySecondFile"), StandardCopyOption.REPLACE_EXISTING);
//                Files.move(fs.getPath("/def/myThirdFile"), fs.getPath("/def/klm"), StandardCopyOption.REPLACE_EXISTING);



//            directoryService.mkdir(new URI("neo4jfs://scsosna98/abc/def"));
//            directoryService.mkdir(new URI("neo4jfs://scsosna98/abc/yui"));
//            directoryService.mkdir(new URI("neo4jfs://scsosna98/abc/def/qqq"));
//            var p = directoryService.parent(new URI("neo4jfs://scsosna98/abc/def/qqq"));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                drop(new URI("neo4jfs://scsosna99/"));
            } catch (Exception e) {

            }
        }
    }
}
