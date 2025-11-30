package dev.scottsosna.neo4jfs.service.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class LocalStorageTreeDeleteVisitor extends SimpleFileVisitor<Path> {

    /**
     * Delete every file found in the directory
     * @param file file being visited
     * @param attrs file attributes (ignored)
     * @return when successful, FileVisitResult.CONTINUE
     * @throws IOException thrown when file cannot be deleted
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException
    {
        file.toFile().delete();
        return FileVisitResult.CONTINUE;
    }

    /**
     * Deletes the directory itself which must occur after all contents (files and subdirectories) have been deleted.
     * @param dir directory being visited
     * @param e when present, exception that prevented a file or subdirectory from being deleted.
     * @return when successful, FileVisitResult.CONTINUE
     * @throws IOException thrown when directory cannot be deleted
     */
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e)
        throws IOException
    {
        if (e == null) {
            dir.toFile().delete();
            return FileVisitResult.CONTINUE;
        } else {
            // directory iteration failed
            throw e;
        }
    }
}
