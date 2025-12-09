package dev.scottsosna.neo4jfs.demo;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Demo01: Create a new Neo4Jfs file system, create directories, load files.  Nothing too fancy.
 */
@Service("demo01")
public class Demo01 implements Demo  {

    @Override
    public void demo() {
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of())) {
            //  Create directories for different media types.
            Files.createDirectory(fs.getPath("/songs"));
            Files.createDirectory(fs.getPath("/videos"));
            Files.createDirectory(fs.getPath("/images"));
            Files.createDirectory(fs.getPath("/text"));
            Files.createDirectory(fs.getPath("/copies"));

            //  Upload images: note that cross-filesystem copies check for existence of target unless
            //  {@code StandardCopyOption.REPLACE_EXISTING} is specified.  However, the existence check does not
            //  know whether the target is a file or directory, therefore must explicitly state target name.
            Files.copy(Path.of("data/IMG_0308.jpg"), fs.getPath("/images/IMG_0308.jpg"));
            Files.copy(Path.of("data/IMG_0324.jpg"), fs.getPath("/images/IMG_0324.jpg"));
            Files.copy(Path.of("data/IMG_0398.jpg"), fs.getPath("/images/IMG_0398.jpg"));

            //  Make copies of original images in the "copies" directory"  Within a file system, the move
            //  can happen into a directory correctly.
            Files.copy(fs.getPath("images/IMG_0308.jpg"), fs.getPath("copies"));
            Files.copy(fs.getPath("images/IMG_0324.jpg"), fs.getPath("copies"));
            Files.copy(fs.getPath("images/IMG_0398.jpg"), fs.getPath("copies"));

            //  Move the original directories into a "data" directory.
            Files.createDirectory(fs.getPath("/data"));
            Files.move(fs.getPath("/songs"), fs.getPath("/data"));
            Files.move(fs.getPath("/videos"), fs.getPath("/data"));
            Files.move(fs.getPath("/images"), fs.getPath("/data"));
            Files.move(fs.getPath("/copies"), fs.getPath("/data"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
