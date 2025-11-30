package dev.scottsosna.neo4jfs.database.repository.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * For debugging purposes, debug logs the directory structure as visited by tree walker.
 *
 * @see java.nio.file.FileVisitor
 * @see Neo4jfsTreeWalker
 */
public class DebuggingFileVisitor extends SimpleFileVisitor<Neo4jfsTreeWalker.NeofjfsWalkerEvent> {

    //  How many directories deep are we.
    private int depth = 0;

    //  Logger used for printing out directory structure.
    private static final Logger logger = LoggerFactory.getLogger(DebuggingFileVisitor.class);

    //  Format used for SLF4J using substitution parameters.
    private static final String SLF4J_FORMAT = "{}{} {}";

    // Pre-built strings for indentation, more performant than repeatedly generating them.
    private static final String[] LEADING_SPACES = {
        "",
        "  ",
        "    ",
        "      ",
        "        ",
        "          ",
        "            ",
        "              ",
        "                ",
        "                  ",
        "                    ",
        "                      ",
        "                        ",
        "                          ",
        "                            ",
        "                              ",
        "                                ",
        "                                  ",
        "                                    ",
        "                                      ",
        "                                        "
    };

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
        logger.debug(SLF4J_FORMAT, indent(depth), "FILE", file.getUri());
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
        logger.debug(SLF4J_FORMAT, indent(depth), "--->", dir.getUri());
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
            logger.debug(SLF4J_FORMAT, indent(depth), "<---", dir.getUri());
            depth--;
            return FileVisitResult.CONTINUE;
        } else {
            // directory iteration failed
            throw e;
        }
    }

    /**
     * @return leading spaces based on depth.
     */
    private static String indent(int depth) {
        return (depth < LEADING_SPACES.length) ? LEADING_SPACES[depth] : LEADING_SPACES[LEADING_SPACES.length - 1];
    }
}
