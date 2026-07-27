/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.database.repository.util;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.exception.Neo4jfsException;
import dev.scottsosna.neo4jfs.exception.Neo4jfsUnknownEntryException;
import dev.scottsosna.neo4jfs.service.DirectoryService;
import dev.scottsosna.neo4jfs.util.SpringContext;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.NEO4JFS_PROPERTY_PAGINATION_SIZE;

/**
 * Neo4Jfs-specific tree walker that works with Neo4Jfs-specific visitors.  More optimized (I believe!) than using
 * {@code java.nio.file.Files.walkFileTree(Path, FileVisitor)}
 */
public class Neo4jfsTreeWalker implements Closeable {

    /**
     * No more work possible when tree walked closed.
     */
    private boolean closed;

    /**
     * The maximum directory depth visited.
     */
    private final int maxDepth;

    private final DirectoryService service;

    /**
     * Directories added to stack when entering subdirectory; removed when leaving subdirectory.  Basically
     * tracks state of where we are in the tree.
     */
    private final ArrayDeque<Neo4jfsWalkerData> stack = new ArrayDeque<>();

    /**
     * Returned when tree walk has completed.
     */
    private static final NeofjfsWalkerEvent EVENT_FINISHED = new NeofjfsWalkerEvent(EventType.FINISHED, null, null, null, null);

    /**
     * Pagination: number of children entries retrieved from Neo4J for each query.
     */
    private final int paginationMaxPerCall;

    /**
     * Event types are essentially the states achieved when walking the tree.
     */
    public enum EventType {
        ENTER_DIRECTORY,    //  Begin processing a directory.
        EXIT_DIRECTORY,      //  Finished processing a directory.
        FILE,               //  A single file or entry in the tree structure
        FINISHED            //  Walking the tree has completed.
    }

    /**
     * Details of life cycle events that are returned to caller as the tree is walked.
     * NOTE: Properties are finalized to prevent external modification.
     */
    public record NeofjfsWalkerEvent (
        EventType eventType,
        DirectoryEntry directory,
        FileEntry file,
        URI uri,
        IOException exception) {
    }

    /**
     * Data for current directory being processed is tracked with stack, maintained as walker navigates up and down
     * the tree.  The iterators are very important for ensuring each file contained or each subdirectory is visited.
     */
    static class Neo4jfsWalkerData {
        private final URI uri;
        private final DirectoryEntry dir;
        private final Iterator<FileEntry> files;
        private final Iterator<DirectoryEntry> subdirs;
        private int skipped;

        /**
         * Constructor
         * @param uri Neo4Jfs file system URI
         * @param dir starting directory for walking the trees
         * @param skipped
         */
        public Neo4jfsWalkerData(final URI uri,
                                 final DirectoryEntry dir,
                                 final int skipped) {
            this.uri = uri;
            this.dir = dir;
            this.files = (dir.getFiles() != null) ? dir.getFiles().iterator() : Collections.emptyIterator();
            this.subdirs = (dir.getSubdirs() != null) ? dir.getSubdirs().iterator() : Collections.emptyIterator();
            this.skipped = skipped;
        }
    }

    /**
     * Constructor.
     *
     * @param maxDepth  The maximum directory depth visited.
     */
    public Neo4jfsTreeWalker(final int maxDepth) {
        if (maxDepth < 0)
            throw new IllegalArgumentException("'maxDepth' is negative");

        this.closed = false;
        this.maxDepth = maxDepth;
        this.service = SpringContext.getBean(DirectoryService.class);
        this.paginationMaxPerCall = SpringContext.getPropertyInteger(NEO4JFS_PROPERTY_PAGINATION_SIZE);
    }

    /**
     * Constructor.
     */
    public Neo4jfsTreeWalker() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Begin walking the tree
     *
     * @param uri starting point in tree
     * @return event for first starting node encountered
     */
    public NeofjfsWalkerEvent walk(URI uri) throws IOException {
        //  Walker has been closed, no more work to do, even when tree was not completely navigated.
        if (closed) {
            throw new IllegalStateException("Tree walker is closed.");
        }

        //  A non-empty stack implies that walking has already started.
        if (!stack.isEmpty()) {
            throw new IllegalStateException("Existing walk already in progress.");
        }

        //  Walking may start anywhere in tree - root, directory, file, link, etc. - so find starting node and
        //  create appropriate event based on its type (today either directory or file).
        BaseEntry startingNode = getStartingEntry(uri);
        switch (startingNode) {
            case DirectoryEntry d:
                //  The node returned by getStartingEntry() does not return directories files or subdirectories, so
                //  retrieve them separately.  The results are paginated to prevent problems with large file systems.
                //  TODO: Need to fix findChildren call
                DirectoryEntry withChildren = null; //service.findChildren(uri, d, 0, paginationMaxPerCall);
                if (withChildren == null) {
                    //  Current directory has no subdirs or files so substitute the starting node so walk can continue.
                    withChildren = d;
                }

                //  Push data for current directory onto stack to track where we are/what we're doing.
                stack.push(new Neo4jfsWalkerData(uri, withChildren, paginationMaxPerCall));

                //  Initial event when walk starts with directory is START_DIRECTORY.
                return new NeofjfsWalkerEvent(EventType.ENTER_DIRECTORY, withChildren, null, uri, null);
            case FileEntry f:
                //  Starting point for file is itself with nothing else to do, no directory to push onto stack,
                //  just process the file and stop.  Only scenario when "directory" of event is null.
                return new NeofjfsWalkerEvent(EventType.FILE, null, f, uri, null);
            default:
                //  Hmmmm, what are we getting?
                throw new Neo4jfsUnknownEntryException(uri, startingNode.getClass().getName());
        }
    }

    /**
     * Continue walking the tree
     *
     * @param env event returned by initial walk() or most recent event returned by next().
     * @return next event in tree walk.
     */
    public NeofjfsWalkerEvent next(NeofjfsWalkerEvent env) throws IOException {
        //  Walker has been closed, no more work to do, even when tree was not completely navigated.
        if (closed) {
            throw new IllegalStateException("Tree walker is closed.");
        }

        //  Nothing else to walk if previous event is that walk completed.
        if (env.eventType == EventType.FINISHED) {
            return env;
        }

        return visit(env);
    }

    /**
     * Return the node/entry for the starting point for walking the tree.
     *
     * @param uri URI of an entry in the tree
     * @return the node.
     */
    private BaseEntry getStartingEntry(URI uri) throws IOException {

        //  Query returns nodes for all parts of the pathname defined in the URI with an empty list returned
        //  when any ancestor does not exist.
        List<BaseEntry> nodes = service.find(uri);
        if (nodes.isEmpty()) {
            throw new NoSuchFileException(uri.toString());
        }

        //  Query structure _should_ guarantee last node is our starting point.  Quick check to
        //  ensure names match but otherwise we'll go with it.  Need special case check when starting
        //  from root as no file name is present.
        BaseEntry lastNode = nodes.getLast();
        Path fileName = Path.of(uri).getFileName();
        if ((fileName != null && !fileName.toString().equals(lastNode.getName())) ||
            (fileName == null && lastNode instanceof DirectoryEntry d && !d.getName().equals(Neo4jfsConstants.NAME_ROOT_DIRECTORY))) {
            throw new Neo4jfsException("Node name/file name mismatch:");
        }

        //  Should be the starting point for the tree walk.
        return lastNode;
    }

    /**
     * Navigate the tree and determine next event to return.
     * @param event previous event provided by caller
     * @return next event in tree walk.
     */
    private NeofjfsWalkerEvent visit(final NeofjfsWalkerEvent event) throws IOException {
        //  Empty stack means we're done.
        Neo4jfsWalkerData current = stack.peek();
        if (current == null) {
            return EVENT_FINISHED;
        }

        //  Reload next page of data when current page exhausted and assign to the current node being visited.
        current = visitReload(current);

        //  Visit next remaining file in current directory, if any.
        NeofjfsWalkerEvent eventToReturn = visitFiles(current);
        if (eventToReturn != null) return eventToReturn;

        //  Visit next remaining subdirectory in current directory, if any.
        eventToReturn = visitSubdirectories(current);
        if (eventToReturn != null) return eventToReturn;

        //  current directory exhausted, generate exit directory event to return to caller.
        return visitExitDirectory(event);
    }

    /**
     * Directory exhausted, no more files or subdirectories to process, multiple paths to exist directory.
     * @param event most recent event returned by next()
     * @return EXIT_DIRECTORY event
     */
    private NeofjfsWalkerEvent visitExitDirectory(final NeofjfsWalkerEvent event) throws IOException {
        //  No files or subdirs left to process, so either we've completed the current tree OR we're done because there's
        //  nothing left on the stack.
        if (stack.isEmpty()) {
            return EVENT_FINISHED;
        } else {
            DirectoryEntry directoryEntry = null;
            URI uri = null;
            Path path = null;
            Neo4jfsWalkerData popped = stack.pop();

            //  Handling based on event type and where we are in the tree
            if (!stack.isEmpty()) {
                //  Current directory walked, ascending up to parent directory.
                switch (event.eventType) {
                    case FILE:
                        //  Last child processed in directory was parent.
                        directoryEntry = stack.peek().dir;
                        uri = popped.uri;
                        break;
                    case ENTER_DIRECTORY:
                        //  Empty directory, no files or subdirs, so immediately ascend out of it
                        directoryEntry = popped.dir;
                        uri = event.uri;
                        break;
                    case EXIT_DIRECTORY:
                        //  Left directory, ascended, and immediately left that directory.
                        directoryEntry = popped.dir;
                        uri = popped.uri;
                        break;
                    default:
                        throw new Neo4jfsUnknownEntryException(event.eventType.name());
                }
            } else {
                //  Empty stack indicates we've walked all the way down and ascended back to where we started, means
                //  the directory popped is our starting point.  Tricky part is gettng the right parent.
                directoryEntry = popped.dir;
                switch (event.eventType) {
                    case FILE:
                    case EXIT_DIRECTORY:
                        uri = popped.uri;
                        break;
                    case ENTER_DIRECTORY:
                        uri = event.uri;
                        break;
                    default:
                        throw new Neo4jfsUnknownEntryException(event.eventType.name());
                }
            }

            //  Create END_DIRECTORY event to be returned.
            return new NeofjfsWalkerEvent(EventType.EXIT_DIRECTORY, directoryEntry, null, uri, null);
        }
    }

    /**
     * Visit the next available file in the current directory.
     *
     * @param current current directory being walked
     * @return FILE event or null if no more files to process.
     */
    private NeofjfsWalkerEvent visitFiles(final Neo4jfsWalkerData current) {

        //  Process all files in a directory before navigating to subdirectories.
        if (current.files.hasNext()) {
            FileEntry fe = current.files.next();
            URI uri = buildWithChild(current.uri, fe.getName());
            return new NeofjfsWalkerEvent(EventType.FILE, current.dir, fe, uri, null);
        }

        return null;
    }

    /**
     * Lazy-loading Neo4J nodes means the file/subdirectory iterators may be exhausted.  When exhausted, attempt to get
     * the next page of nodes from Neo4J.  Pagination is only applicable for large file systems.
     * @param current current directory being walked.
     */
    private Neo4jfsWalkerData visitReload(final Neo4jfsWalkerData current) {
        //  Both iterators exhausted may simply mean that current page of children objects are exhausted and more are
        //  available.  Attempt to retrieve more.
        if (!current.files.hasNext() && !current.subdirs.hasNext()) {
            DirectoryEntry withChildren = null; // repository.getChildren(current.uri, current.dir.getId(), current.skipped, paginationMaxPerCall);

            //  It's possible - likely with anything but an extremely large file system - that the directory has no
            //  additional files, subdirs, etc. to process in which case we'll simply drop out farther down.
            if (withChildren != null && (
                (withChildren.getFiles() != null && !withChildren.getFiles().isEmpty()) ||
                    (withChildren.getSubdirs() != null && !withChildren.getSubdirs().isEmpty()))) {
                stack.pop();
                Neo4jfsWalkerData reloaded = new Neo4jfsWalkerData(current.uri, withChildren, current.skipped + paginationMaxPerCall);
                stack.push(reloaded);
                return reloaded;
            }
        }

        return current;
    }

    /**
     * Visit the next available subdirectory of the current directory
     *
     * @param current current directory being walked
     * @return START_DIRECTORY event or null if no more subdirectories to process.
     */
    private NeofjfsWalkerEvent visitSubdirectories(final Neo4jfsWalkerData current) throws IOException {

        //  Assuming requested max depth has not been reached, visit next subdirectory.
        if (current.subdirs.hasNext() && stack.size() <= maxDepth) {
            DirectoryEntry subdir = current.subdirs.next();

            //  Construct URI for subdirectory and attempt to retrieve.
            URI uri = buildWithChild(current.uri, subdir.getName());
            BaseEntry subdirEntry = getStartingEntry(uri);

            //  Entry retrieve _should_ be a directory, but just in case...
            if (subdirEntry instanceof DirectoryEntry d) {
                //  Re-retrieve node, this time gettings its children as well.
                DirectoryEntry subdirAndChildren = null; // repository.getChildren(uri, d.getId(), 0, paginationMaxPerCall);
                if (subdirAndChildren == null) {
                    subdirAndChildren = d;
                }

                //  Push subdir onto stack.
                stack.push(new Neo4jfsWalkerData(uri, subdirAndChildren, paginationMaxPerCall));

                //  Return event for starting a new directory.
                URI eventUri = buildWithChild(current.uri, subdirEntry.getName());
                return new NeofjfsWalkerEvent(EventType.ENTER_DIRECTORY, subdirAndChildren, null, eventUri, null);
            } else {
                //  Indicates a corrupted tree/graph, most likely duplicately named directories or files exist.  Bad.
                throw new Neo4jfsUnknownEntryException(uri, subdirEntry.getClass().getName());
            }
        }

        return null;
    }

    /**
     * Correctly build a URI, accounting for starting at root where concatenating separator not needed.
     *
     * @param current current URI
     * @param childName name of the subdirectory or file in directory.
     * @return new URI with the subdirectory.
     */
    private URI buildWithChild(URI current, String childName) {
        if (current.getPath().endsWith(Neo4jfsConstants.PATH_SEPARATOR)) {
            return URI.create(current + childName);
        } else {
            return URI.create(current + Neo4jfsConstants.PATH_SEPARATOR + childName);
        }
    }

    /**
     * Tree walker is closed and no longer usable.
     * @throws IOException not thrown but defined in parent interface.
     */
    @Override
    public void close() throws IOException {
        closed = true;
    }
}
