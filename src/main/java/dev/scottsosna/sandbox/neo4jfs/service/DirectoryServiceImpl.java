package dev.scottsosna.sandbox.neo4jfs.service;

import dev.scottsosna.sandbox.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.sandbox.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.sandbox.neo4jfs.database.node.FileEntry;
import dev.scottsosna.sandbox.neo4jfs.database.repository.DirectoryEntryRepository;
import dev.scottsosna.sandbox.neo4jfs.database.repository.util.DebuggingFileVisitor;
import dev.scottsosna.sandbox.neo4jfs.database.repository.util.DirectoryDeleteFileVisitor;
import dev.scottsosna.sandbox.neo4jfs.database.repository.util.Neo4jfsFileAttributes;
import dev.scottsosna.sandbox.neo4jfs.database.repository.util.Neo4jfsTreeWalker;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DirectoryServiceImpl extends BaseNeo4jfsService implements DirectoryService {

    private final DirectoryEntryRepository repository;
    private FileService fileService;
    private final Map<String, FileVisitor> visitorMap = new HashMap<>();

    public DirectoryServiceImpl(DirectoryEntryRepository repository) {
        this.repository = repository;
    }

    public DirectoryEntry createRoot (URI uri) {
        checkSchema(uri);
        return repository.createRoot(uri);
    }

    public DirectoryEntry findOrCreateRoot(URI uri) {
        checkSchema(uri);
        DirectoryEntry d = repository.findRoot(uri);
        if (d == null) {
            d = repository.createRoot(uri);
        }

        return d;
    }

    public DirectoryEntry mkdir (URI uri) {
        checkSchema(uri);

        //  The parents of the new directory must exist, so query them from the database.
        Path path = Path.of(uri);
        Path parent = path.getParent();
        List<BaseEntry> entries = repository.find(uri, parent);
        if (entries.isEmpty()) {
            throw new RuntimeException("%s: no such file or directory".formatted(parent));
        }

        //  Ensure that immediate parent entry of the new directory is a directory.
        if (entries.getLast() instanceof DirectoryEntry dir) {
            //  Make sure the name requested doesn't already exist
            BaseEntry child = repository.findNamedChild(uri, dir.getId(), path.getFileName().toString());
            if (child != null) {
                throw new RuntimeException("%s: File already exists".formatted(path));
            }

            //  All good, create the new directory.
            DirectoryEntry newbie = repository.create(uri, path.getFileName().toString());
            dir.setSubdirs(List.of(newbie));
            repository.save(uri, dir);
            return newbie;
        } else {
            //  Not a directory, fail.
            throw new RuntimeException("%s: Not a directory".formatted(parent));
        }
    }

    /**
     * Delete node specified by URI, file or directory
     * @param uri Neo4jfs URI specifying either file or directory to delete.
     * @throws IOException thrown for unresolved pathname or an error doing delete.
     */
    public void delete(URI uri) throws IOException {

        //  Get the requested pathname as ordered list of nodes.
        List<BaseEntry> parts = prologue(uri);

        //  Last part must be a directory.
        BaseEntry lastPart = parts.getLast();
        switch (lastPart) {
            case FileEntry f:
                fileService.delete(uri);
                break;
            case DirectoryEntry d:
                rmdirWork(uri, lastPart);
                break;
            default:
                throw new RuntimeException("%s: Unknown node type".formatted(uri));
        }
    }

    /**
     * Deletes (removes) an empty directory specified by URI, similar to *nix {@code rmdir} command
     * @param uri Neo4jfs URI specifying directory to delete
     * @throws IOException an error occurred, such as directory not empty.
     */
    public void rmdir(URI uri) throws IOException {

        //  Get the requested pathname as ordered list of nodes.
        List<BaseEntry> parts = prologue(uri);

        //  Method does work of ensuring directory is empty before deleting it.
        rmdirWork(uri, parts.getLast());
    }

    /**
     * Walks directory from node specified by URI and deletes everything bottom-up, similar to *nix {@code rm -rf} command
     * @param uri Neo4jfs URI for the directory or file to delete.
     * @throws IOException
     */
    public void rmdirRecursively(URI uri) throws IOException {

        //  Get the requested pathname as ordered list of nodes.
        List<BaseEntry> parts = prologue(uri);

        //  Last part must be a directory.
        BaseEntry lastPart = parts.getLast();
        switch(lastPart) {
            case FileEntry f:
                //  Following pattern used by file-based file system where "rmrecursively" implies straight
                //  file delete.
                fileService.delete(uri);
                break;
            case DirectoryEntry d:
                //  Delete all children recursively.
                walkFileTree(uri, DirectoryDeleteFileVisitor.VISITOR_KEY);
                break;
            default:
                throw new RuntimeException("%s: Unknown node type".formatted(uri));
        }
    }

    public void dumpTree(URI uri) {
        checkSchema(uri);
        walkFileTree(uri, new DebuggingFileVisitor());
    }

    public BaseEntry parent(URI uri) {
        checkSchema(uri);
        return repository.parent(uri);
    }

    public List<BaseEntry> find(URI uri) {
        checkSchema(uri);
        return repository.find(uri, Path.of(uri));
    }

    public DirectoryEntry findChildren(URI uri, String parentId, int skip, int limit) {
        checkSchema(uri);
        return repository.getParentWithChildren(uri, parentId, skip, limit);
    }

    public DirectoryEntry addFile(URI uri, DirectoryEntry parent, FileEntry file) {
        checkSchema(uri);
        parent.setFiles(List.of(file));
        return repository.save(uri, parent);
    }

    public DirectoryEntry getRoot(URI uri) {
        checkSchema(uri);
        DirectoryEntry d = repository.findRoot(uri);
        return d;
    }

    /**
     * Initial steps for a number of operations where the tree is required before proceeding
     * @param uri Neo4jfs URI for the directory path on which to operate
     * @return the nodes representing the path.
     * @throws IOException if the path doesn't exist.
     */
    private List<BaseEntry> prologue(URI uri) throws IOException{
        checkSchema(uri);

        //  Confirm path existence.
        List<BaseEntry> parts = find(uri);
        if (parts.isEmpty()) {
            throw new FileNotFoundException("%s: no such file or directory".formatted(uri));
        }

        return parts;
    }

    private void rmdirWork(URI uri, BaseEntry lastPart) throws IOException {
        if (!(lastPart instanceof DirectoryEntry)) {
            throw new RuntimeException("%s: Not a directory".formatted(uri));
        }

        //  Only empty directories are deleted/removed.
        DirectoryEntry entry = repository.getParentWithChildren(uri, lastPart.getId(), 0, 2);
        if (entry == null ||
            ((entry.getFiles() == null || entry.getFiles().isEmpty()) &&
                (entry.getSubdirs() == null || entry.getSubdirs().isEmpty()))) {
            repository.delete(uri, lastPart.getId());
        } else {
            throw new RuntimeException("%s: Directory not empty".formatted(uri));
        }
    }

    /**
     * Walk the Neo4J file system tree and apply the visitor to each node (file, directory, etc)
     * based on the event type
     * @param uri starting point in tree
     * @param visitor visitor to apply to each node
     */
    private void walkFileTree(URI uri, FileVisitor<Neo4jfsTreeWalker.NeofjfsWalkerEvent> visitor) {
        checkSchema(uri);

        var attribs = new Neo4jfsFileAttributes();

        try (var walker = new Neo4jfsTreeWalker(repository)) {
            var env = walker.walk(uri);
            do {
                switch (env.getEventType()) {
                    case FILE:
                        visitor.visitFile(env, attribs);
                        break;
                    case ENTER_DIRECTORY:
                        visitor.preVisitDirectory(env, attribs);
                        break;
                    case EXIT_DIRECTORY:
                        visitor.postVisitDirectory(env, null);
                        break;
                    default:
                        break;
                }
                env = walker.next(env);
            } while (env.getEventType() != Neo4jfsTreeWalker.EventType.FINISHED);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Walk the Neo4J file system tree and apply the visitor to each node (file, directory, etc)
     * based on the event type
     * @param uri starting point in tree
     * @param fileVisitorKey key for finding registered visitor
     */
    private void walkFileTree(URI uri, String fileVisitorKey) {
        checkSchema(uri);
        walkFileTree(uri, visitorMap.get(fileVisitorKey));
    }

    public void registerVisitor(final String key, final FileVisitor visitor) {
        visitorMap.put(key, visitor);
    }

    /**
     * Avoids circular references between FileService and DirectoryService by having FileService register itself.
     * @param fileService
     */
    public void registerFileService(FileService fileService) {
        this.fileService = fileService;
    }
}
