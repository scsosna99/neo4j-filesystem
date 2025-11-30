package dev.scottsosna.neo4jfs.database.repository.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class DebuggingFileVisitor extends SimpleFileVisitor<Neo4jfsTreeWalker.NeofjfsWalkerEvent> {

    //  How many directories deep are we.
    private int depth = 0;

    /**
     * Delete every file found in the directory
     * @param file file being visited
     * @param attrs file attributes (ignored)
     * @return  FileVisitResult.CONTINUE
     * @throws IOException thrown when file cannot be processed
     */
    @Override
    public FileVisitResult visitFile(Neo4jfsTreeWalker.NeofjfsWalkerEvent file, BasicFileAttributes attrs)
        throws IOException
    {
        depth++;
        System.out.printf(formatIndent(), "FILE", file.getUri());
        depth--;
        return FileVisitResult.CONTINUE;
    }

    /**
     * Log that directory has been entered.
     * @param dir directory being visited
     * @return always FileVisitResult.CONTINUE
     * @throws IOException thrown when directory cannot be exited
     */
    @Override
    public FileVisitResult preVisitDirectory(Neo4jfsTreeWalker.NeofjfsWalkerEvent dir, BasicFileAttributes attrs) throws IOException{
        depth++;
        System.out.printf(formatIndent(), "--->", dir.getUri());
        return FileVisitResult.CONTINUE;
    }

    /**
     * Log that directory has been exited.
     * @param dir directory being visited
     * @param e when present, exception that prevented a file or subdirectory from being visited.
     * @return always FileVisitResult.CONTINUE
     * @throws IOException thrown when directory cannot be exited
     */
    @Override
    public FileVisitResult postVisitDirectory(Neo4jfsTreeWalker.NeofjfsWalkerEvent dir, IOException e)
        throws IOException
    {
        if (e == null) {
            System.out.printf(formatIndent(), "<---", dir.getUri());
            depth--;
            return FileVisitResult.CONTINUE;
        } else {
            // directory iteration failed
            throw e;
        }
    }

    /**
     * Creates a dynamic printf string to imply depth through indentation.
     * @return printf string
     */
    private String formatIndent() {
        return (depth == 0) ? "%s %s\n" :"%" + (depth * 4) + "s %s\n";
    }
}
