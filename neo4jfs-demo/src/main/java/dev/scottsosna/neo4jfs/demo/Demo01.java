package dev.scottsosna.neo4jfs.demo;

import dev.scottsosna.neo4jfs.service.FileSystemService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Map;

@ComponentScan("dev.scottsosna.neo4jfs")
@SpringBootApplication
public class Demo01 {

    public static void main(String[] args) {
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4j://mydemo"), Map.of())) {
            Files.createDirectory(fs.getPath("/abc"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
