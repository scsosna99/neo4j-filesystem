/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.demo;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Demo04: Temp directories and files.
 */
@Service("demo04")
public class Demo04 extends Demo  {

    @Override
    public void demo() {
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of())) {

            //  Create a directory for temp files, etc.
            Path baseTemp = fs.getPath("data", "temp");
            Files.createDirectory(baseTemp);

            //  Create a temporary directory.
            Path tempDir = Files.createTempDirectory(baseTemp, "tempdir");

            //  Create a bunch of temp files.
            for (int i = 0; i < 10; i++) {
                Files.createTempFile(tempDir, "tempfile", ".tmp");
            }

            //  Print out file names in the temporary directory.
            Files.newDirectoryStream(tempDir).forEach(path -> {System.out.println(path.toString());});
        } catch (IOException e) {
            e.printStackTrace();
        }
     }
}
