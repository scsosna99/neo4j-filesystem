/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */

package dev.scottsosna.neo4jfs.demo;

import dev.scottsosna.neo4jfs.filesystem.attribute.GroupPrincipalImpl;
import dev.scottsosna.neo4jfs.filesystem.attribute.UserPrincipalImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Map;

/**
 * Demo05: Show Neo4Jfs security in action by creating different users, uploading files, and demonstrating permissions
 * allowing/disallowing operations.  Simple but instructive.
 */
@Slf4j
@Service("demo05")
public class Demo05 extends Demo  {

    @Override
    public void demo() {

        try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of())) {
            createHome(fs);
            setupAlice(fs);
            workByBob(fs);
            copyByCarol(fs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create home directories for each user, change the owner, and update permissions.
     * @param fs Neo4jFS file system instance
     * @throws IOException when an error occurs
     */
    private void createHome(final FileSystem fs) throws IOException {
        try {
            //  Base directory under which each user's home directory is created.
            Files.createDirectory(fs.getPath("/home"));

            //  Permissions for each home directory.
            EnumSet<PosixFilePermission> perms = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.OTHERS_READ
            );

            //  Iterate through users to create and configure their home directories.
            String[] users = {"alice", "bob", "carol"};
            for (String user : users) {
                //  Create home directory.
                Path path = fs.getPath("/home/%s".formatted(user));
                Files.createDirectory(path);

                //  Set the owner to the user
                Files.setOwner(path, new UserPrincipalImpl(user));
                Files.setPosixFilePermissions(path, perms);

                //  Set group to the user
                Files.getFileAttributeView(path, PosixFileAttributeView.class).setGroup(new GroupPrincipalImpl(user));
            }
        } catch (IOException e) {
            System.out.println("Error creating home directories: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * "Alice" creates a shared directory and uploads personal/shared files
     * @param fs Neo4jFS file system instance
     * @throws IOException when an error occurs
     */
    private void setupAlice(final FileSystem fs) throws IOException {
        //  Current user is Alice.
        setSecurityContext("alice", "alice");

        try {
            //  Create shared directory and set permissions so anyone can read/write
            Path path = fs.getPath("/home/alice/shared");
            Files.createDirectory(path);
            Files.setPosixFilePermissions(path, EnumSet.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE
            ));

            //  Alice uploads personal files
            Files.copy(Path.of("data/apples1.jpg"), fs.getPath("/home/alice/apples1.jpg"));
            Files.copy(Path.of("data/apples2.jpg"), fs.getPath("/home/alice/apples2.jpg"));

            //  Alice uploads shared files
            Files.copy(Path.of("data/apples3.jpg"), fs.getPath("/home/alice/shared/apples3.jpg"));
            Files.copy(Path.of("data/apples4.jpg"), fs.getPath("/home/alice/shared/apples4.jpg"));
        } catch (IOException e) {
            System.out.println("Error setting up alice directory: " + e.getMessage());
            throw e;
        }
    }

    /**
     * "Bob" attempts to access Alice's files and then uploads new shared file.
     * @param fs Neo4jFS file system instance
     * @throws IOException when an error occurs
     */
    private void workByBob(final FileSystem fs) throws IOException {
        //  Current user is Bob.
        setSecurityContext("bob", "bob");

        //  Bob attempts to delete Alice's private file, exception should be thrown.
        try {
            //  Bob doesn't have access so expect exception thrown.
            Files.delete(fs.getPath("/home/alice/apples1.jpg"));
        } catch (IOException e) {
            System.out.println("Bob unable to access Alice private file, expected." + e.getMessage());
        }

        //  Bob does have access to shared directory so expect to get the size.
        try {
            //  Bob has access to shared file so delete should succeed
            Files.delete(fs.getPath("/home/alice/shared/apples3.jpg"));
        } catch (IOException e) {
            System.out.println("Bob unable to access Alice shared file, not expected." + e.getMessage());
            throw e;
        }

        //  Bob uploads new shared file.
        try {
            Files.copy(Path.of("data/apples3.jpg"), fs.getPath("/home/alice/shared/apples5.jpg"));
        } catch (IOException e) {
            System.out.println("Bob unable to write to Alice shared directory: " + e.getMessage());
            throw e;
        }

        //  Upload file to personal directory, one shared and one unshaared
        try {
            Path path = fs.getPath("/home/bob/shared_apples.jpg");
            Files.copy(Path.of("data/apples4.jpg"), path);
            Files.setPosixFilePermissions(path, EnumSet.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE
            ));

            path = fs.getPath("/home/bob/unshared_apples.jpg");
            Files.copy(Path.of("data/apples4.jpg"), path);
            Files.setPosixFilePermissions(path, EnumSet.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE
            ));
        } catch (IOException e) {
            System.out.println("Bob unable to write to Bob personal directory: " + e.getMessage());
            throw e;
        }
    }

    /**
     * "Carol" copies shared files to her home directory
     * @param fs Neo4jFS file system instance
     * @throws IOException when an error occurs
     */
    private void copyByCarol(final FileSystem fs) throws IOException {
        //  Current user is Carol.
        setSecurityContext("carol", "carol");

        //  Carol wants her own copy of shared files.
        try {
            Files.copy(fs.getPath("/home/alice/shared/apples5.jpg"), fs.getPath("/home/carol"));
            Files.copy(fs.getPath("/home/bob/shared_apples.jpg"), fs.getPath("/home/carol"));
        } catch (IOException e) {
            System.out.println("Carol unable to copy files: " + e.getMessage());
            throw e;
        }

        //  Carol cannot copy Bob's unshared file.
        try {
            Files.copy(fs.getPath("/home/bob/unshared_apples.jpg"), fs.getPath("/home/carol"));
        } catch (IOException e) {
            System.out.println("expected, Carol unable to copy file: " + e.getMessage());
        }

        //  However, is Carol is member of Bob's group, then copy should succeed
        setSecurityContext("carol", "bob");
        try {
            Files.copy(fs.getPath("/home/bob/unshared_apples.jpg"), fs.getPath("/home/carol"));
        } catch (IOException e) {
            System.out.println("Carol unable to copy file: " + e.getMessage());
            throw e;
        }
    }
}
