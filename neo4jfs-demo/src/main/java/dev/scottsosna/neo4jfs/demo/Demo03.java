/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.demo;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;

/**
 * Demo03: Posix permissions demo where permissions are inherited where they aren't defined on directory/file/.
 */
@Service("demo03")
public class Demo03 extends Demo  {

    @Override
    public void demo() {

        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of())) {
            System.out.println ("'/' permissions: " + Files.getPosixFilePermissions(fs.getPath("/")));
            System.out.println ("'/data/images' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images")));
            System.out.println ("'/data/images/apples1.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples1.jpg")));
            System.out.println ("'/data/images/apples2.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples2.jpg")));
            System.out.println ("'/data/images/apples3.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples3.jpg")));
            System.out.println ("'/data/images/apples4.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples4.jpg")));

            System.out.println("Changing '/data/images' permissions to rwx---rwx");
            Files.setPosixFilePermissions(fs.getPath("/data/images"), Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE)
            );

            System.out.println("Changing '/data/images/apples3.jpg' permissions to ---rwxrwx");
            Files.setPosixFilePermissions(fs.getPath("/data/images/apples3.jpg"), Set.of(
                PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE)
            );

            System.out.println ("'/' permissions: " + Files.getPosixFilePermissions(fs.getPath("/")));
            System.out.println ("'/data/images' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images")));
            System.out.println ("'/data/images/apples1.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples1.jpg")));
            System.out.println ("'/data/images/apples2.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples2.jpg")));
            System.out.println ("'/data/images/apples3.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples3.jpg")));
            System.out.println ("'/data/images/apples4.jpg' permissions: " + Files.getPosixFilePermissions(fs.getPath("/data/images/apples4.jpg")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
