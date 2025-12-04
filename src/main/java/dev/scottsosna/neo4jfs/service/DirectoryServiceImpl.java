package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.database.repository.DirectoryEntryRepository;
import dev.scottsosna.neo4jfs.database.repository.util.DebuggingFileVisitor;
import dev.scottsosna.neo4jfs.database.repository.util.DirectoryDeleteFileVisitor;
import dev.scottsosna.neo4jfs.database.repository.util.Neo4jfsFileAttributes;
import dev.scottsosna.neo4jfs.database.repository.util.Neo4jfsTreeWalker;
import dev.scottsosna.neo4jfs.exception.Neo4jfsUnknownEntryException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.*;

@Service
public class DirectoryServiceImpl extends BaseNeo4jfsService implements DirectoryService {

    private final DirectoryEntryRepository repository;
    private FileService fileService;
    private final Map<String, FileVisitor> visitorMap = new HashMap<>();

    /**
     * Constructor
     * @param repository database repository for managing Directory nodes in Neo4J.
     */
    public DirectoryServiceImpl(DirectoryEntryRepository repository) {
        this.repository = repository;
    }

    /**
     * Each file system needs a root '/' directory that is (somewhat) immutable
     * @param fsUri Neo4Jfs URI for the specific partition
     * @return the newly-created root directory
     */
    public DirectoryEntry createRoot (URI fsUri) {
        checkUri(fsUri);
        return repository.createRoot(fsUri);
    }

    /**
     * Create file system root '/' directory if one doesn't already exist
     * @param fsUri Neo4Jfs URI for the specific partition
     * @return the root directory
     */
    public DirectoryEntry findOrCreateRoot(URI fsUri) {
        checkUri(fsUri);
        DirectoryEntry d = repository.findRoot(fsUri);
        if (d == null) {
            d = repository.createRoot(fsUri);
        }

        return d;
    }

    /**
     * Create new directory in file system
     * @param uri fully-qualified Neo4Jfs URI specifying directory to create
     * @return the newly created directory
     * @throws IOException I/O problem creating the new directory.
     */
    public DirectoryEntry mkdir (URI uri) throws IOException {
        checkUri(uri);

        //  The parents of the new directory must exist, so query them from the database.
        Path path = Path.of(uri);
        Path parent = path.getParent();
        List<BaseEntry> entries = repository.find(uri, parent);
        if (entries.isEmpty()) {
            throw new NoSuchFileException(parent.toString());
        }

        //  Ensure that immediate parent entry of the new directory is a directory.
        if (entries.getLast() instanceof DirectoryEntry dir) {
            //  Make sure the name requested doesn't already exist
            BaseEntry child = repository.findNamedChild(uri, dir.getId(), path.getFileName().toString());
            if (child != null) {
                throw new FileAlreadyExistsException(path.toString());
            }

            //  All good, create the new directory.
            DirectoryEntry newbie = repository.create(uri, path.getFileName().toString());
            dir.setSubdirs(List.of(newbie));
            repository.save(uri, dir);
            return newbie;
        } else {
            //  Parent directory is in fact not a directory, fail.
            throw new NotDirectoryException(parent.toString());
        }
    }

    /**
     * Adds file to the existing directory.
     * @param fsUri Neo4Jfs file system URI
     * @param parent parent/containing directory of the file to add
     * @param file file to add
     * @return updated DirectoryEntry
     */
    public DirectoryEntry addFile(URI fsUri, DirectoryEntry parent, FileEntry file) {
        checkUri(fsUri);
        parent.setFiles(List.of(file));
        return repository.save(fsUri, parent);
    }

    /**
     * Delete node specified by URI, file or directory
     * @param uri Neo4Jfs URI specifying either file or directory to delete.
     * @throws IOException I/O errors such as unresolved pathname or delete failed.
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
                throw new Neo4jfsUnknownEntryException(uri, lastPart.getClass().getName());
        }
    }

    @Override
    public void copy(URI fromUri, URI toUri, CopyOption... options) throws IOException {

    }

    /**
     * Move file and directory within same file system.
     * @param fromUri file or directory to move
     * @param toUri target location.
     * @param options  options specifying how the move should be done
     * @throws IOException problems executing the move.
     */
    @Override
    public void move(URI fromUri, URI toUri, CopyOption... options) throws IOException {
        checkUri(fromUri);
        checkUri(toUri);

        //  Identical URIs, nothing to do.
        if (fromUri.equals(toUri)) {
            return;
        }

        //  Same parent URI means straight rename.
        URI fromParentUri = fromUri.resolve(".");
        URI toParentUri = toUri.resolve(".");
        if (fromParentUri.equals(toParentUri)) {
            //  Do straight rename.
            renameEntry(fromUri, toUri, options);
            return;
        }

        //  The from/source must exist as either file or directory.
        List<BaseEntry> fromParts = prologue(fromUri);
        BaseEntry fromEntry = fromParts.getLast();
        if (fromEntry instanceof DirectoryEntry de && de.isRoot()) {
            throw new AccessDeniedException("%s: Root directory cannot be moved".formatted(fromUri));
        }
        DirectoryEntry fromParent = (DirectoryEntry) fromParts.get(fromParts.size() - 2);

        //  The destination/target _may_ exist; if not, it's parent must as a directory.
        List<BaseEntry> toParts = null;
        BaseEntry toEntry = null;
        try {
            toParts = prologue(toUri);
            toEntry = toParts.getLast();
        } catch (NoSuchFileException nsfe) {
            //  Destination doesn't exist, so its parent must and must be directory.
            toParts = prologue(toParentUri);
            toEntry = toParts.getLast();
            if (toEntry instanceof FileEntry) {
                //  Parent exists but it's a file.
                throw new NotDirectoryException("%s: Not a directory".formatted(toUri));
            }
        }

        //  Based on from/to entry types, determine course of action.
        switch (fromEntry) {
            //  Source is file.
            case FileEntry fe1:
                switch (toEntry) {
                    //  Target is file.
                    case FileEntry fe2:
                        moveFileWork(fromUri, fe1, fromParent, fe2, (DirectoryEntry) toParts.get(toParts.size() - 2), toUri, options);
                        break;
                    //  Target id directory.
                    case DirectoryEntry de2:
                        moveFileWork(fromUri, fe1, fromParent, null, de2, toUri, options);
                        break;
                    default:
                        throw new Neo4jfsUnknownEntryException(toUri, toEntry.getClass().getName());
                }
                break;
            //  Source is directory.
            case DirectoryEntry de:
                switch (toEntry) {
                    //  Target is file.
                    case FileEntry fe2:
                        //  Can't move directory to already existing file.
                        throw new NotDirectoryException("%s: Not a directory".formatted(toUri));
                    //  Target is directory.
                    case DirectoryEntry de2:
                        moveFileWork(fromUri, de, fromParent, null, de2, toUri, options);
                        break;
                    default:
                        throw new Neo4jfsUnknownEntryException(toUri, toEntry.getClass().getName());
                }
                break;
            default:
                throw new Neo4jfsUnknownEntryException(fromUri, fromEntry.getClass().getName());
        }
    }

    /**
     * Deletes an empty directory specified by URI, similar to *nix {@code rmdir} command.
     * @param uri Neo4Jfs URI specifying directory to delete
     * @throws IOException error occurred, such as directory not empty.
     */
    public void rmdir(URI uri) throws IOException {

        //  Get the requested pathname as ordered list of nodes.
        List<BaseEntry> parts = prologue(uri);

        //  Method does work of ensuring directory is empty before deleting it.
        rmdirWork(uri, parts.getLast());
    }

    /**
     * Walk directory from starting point specified by URI and deletes files/directories/everything
     * bottom-up, similar to *nix {@code rm -rf} command
     * @param uri Neo4Jfs URI for the directory or file to delete.
     * @throws IOException I/O error occurred while deleting tree.
     */
    public void rmdirRecursively(URI uri) throws IOException {

        //  Get the requested pathname as ordered list of nodes.
        List<BaseEntry> parts = prologue(uri);

        //  Last part must be a directory.
        BaseEntry lastPart = parts.getLast();
        switch(lastPart) {
            case FileEntry f:
                //  Following pattern used by file-based file system where "rmdir recursively" implies straight
                //  file delete.
                fileService.delete(uri);
                break;
            case DirectoryEntry d:
                //  Delete all children recursively.
                walkFileTree(uri, DirectoryDeleteFileVisitor.VISITOR_KEY);
                break;
            default:
                throw new Neo4jfsUnknownEntryException(uri, lastPart.getClass().getName());
        }
    }

    /**
     * Walks tree, dumping file structure to logger
     * @param uri Neo4Jfs URI for the directory to dump
     * @throws IOException I/O error occurred while walking tree
     */
    public void dumpTree(URI uri) throws IOException {
        checkUri(uri);
        walkFileTree(uri, new DebuggingFileVisitor());
    }

    public BaseEntry parent(URI uri) {
        checkUri(uri);
        return repository.parent(uri);
    }

    public List<BaseEntry> find(URI uri) {
        checkUri(uri);
        return repository.find(uri, Path.of(uri));
    }

    public DirectoryEntry findChildren(URI uri, String parentId, int skip, int limit) {
        checkUri(uri);
        return repository.getParentWithChildren(uri, parentId, skip, limit);
    }

    /**
     * Returns the entry specified by URI as BasicFileAttributeView, needed by file system provider.
     * @param uri Neo4Jfs URI for the directory or file to read attribute view for.
     * @param options ignored
     * @return the attributes as a "view"
     * @throws IOException I/O error occurred while retrieving the entry to return
     */
    public BasicFileAttributeView readAttributeView(URI uri, LinkOption... options) throws IOException {
        checkUri(uri);
        return find(uri).getLast();
    }

    /**
     * Attempt to set attribute with value provided
     * @param uri Neo4Jfs URI for directory/file to set attribute.
     * @param viewName name of view to modify
     * @param attribute attribute name to modify
     * @param value new attribute values
     * @param options options for any linked entries
     * @throws IOException if I/O error occurs
     */
    public void setAttribute(URI uri,
                             String viewName,
                             String attribute,
                             Object value,
                             LinkOption... options) throws IOException {
        checkUri(uri);
        BaseEntry entry = find(uri).getLast();

        switch (viewName) {
            case ATTRIBUTE_VIEW_NAME_BASIC:
                setAttributeBasic(entry, attribute, value);
                break;
            default:
                //  Generally view name has already been validated, but just in case.
                throw new IllegalArgumentException("Setting attribute not supported for view: %s".formatted(attribute));
        }

        //  Once here we know something modified as otherwise an exception is thrown.
        repository.save(uri, entry, BaseEntry.class);
    }

    /**
     * Check for file/directory exists
     * @param uri Neo4Jfs URI for the directory or file to check
     * @return true if exists, false otherwise.
     */
    public boolean exists(URI uri) {
        checkUri(uri);
        return repository.pathExists(uri);
    }

    /**
     * Moves file from one directory to another.
     * @param fsUri Neo4Jfs URI for the specific partion.
     * @param from source file being moved
     * @param fromParent parent directory of source file
     * @param to when not null, target file being replaced as part of move
     * @param toParent parent directory of target file
     * @param options copy options to apply
     * @throws IOException something bad has happened and move can't proceed.
     */
    private void moveFileWork(final URI fsUri,
                              final BaseEntry from,
                              final DirectoryEntry fromParent,
                              final FileEntry to,
                              final DirectoryEntry toParent,
                              final URI toUri,
                              CopyOption... options) throws IOException {

        //  If a to/destination file exists, move fails UNLESS options say overwriting is OK.
        if (to != null && !checkForOption(StandardCopyOption.REPLACE_EXISTING, options)) {
            throw new FileAlreadyExistsException(to.getName());
        }

        //  Add the to/source file to the to/destination directory.
        switch (from) {
            case FileEntry fe:
                toParent.setFiles(List.of(fe));
                break;
            case DirectoryEntry de:
                toParent.setSubdirs(List.of(de));
                break;
            default:
                throw new Neo4jfsUnknownEntryException(fsUri, from.getClass().getName());
        }
        repository.save(fsUri, toParent);

        //  Remove the from/source file from the from/source directory.
        repository.deleteRelationship(fsUri, fromParent.getId(), from.getId());

        //  Final step is to delete the original to/destination file if it existed.
        if (to != null) {
            fileService.delete(fsUri, to.getId());
        }

        String targetName = Path.of(toUri).getFileName().toString();
        if (!from.getName().equals(targetName)) {
            //  Rename the node.
            from.setName(targetName);
            repository.save(fsUri, from, BaseEntry.class);
        }

    }

    /**
     * Initial steps for a number of operations where the tree is required before proceeding
     * @param uri Neo4Jfs URI for the directory path on which to operate
     * @return the nodes representing the path.
     * @throws IOException if the path doesn't exist.
     */
    private List<BaseEntry> prologue(URI uri) throws IOException{
        checkUri(uri);

        //  Confirm path existence.
        List<BaseEntry> parts = find(uri);
        if (parts.isEmpty()) {
            throw new NoSuchFileException("%s: no such file or directory".formatted(uri));
        }

        return parts;
    }

    private void renameEntry(URI fromUri, URI toUri, CopyOption... options) throws IOException {

        //  Get the requested node to rename.
        List<BaseEntry> fromParts = prologue(fromUri);

        boolean toExists = repository.pathExists(toUri);
        if (toExists) {
            //  It a file or directory already exists with the target name, options must be provided
            //  that allows replacing the original (essentially deleting it).
            if (!checkForOption(StandardCopyOption.REPLACE_EXISTING, options)) {
                throw new FileAlreadyExistsException("%s: file exists".formatted(toUri));
            }

            //  Target must be deleted before we rename the source node.  When the existing node is a
            //  directory, recursively subtree and delete everything underneath.
            rmdirRecursively(toUri);
        }

        //  Rename the node.
        BaseEntry entry = fromParts.getLast();
        entry.setName(Path.of(toUri).getFileName().toString());
        repository.save(fromUri, entry, BaseEntry.class);
    }

    /**
     * Where the real work of deleting a directory happens
     * @param uri file system's URI by specifying the partition
     * @param lastPart the specific entry to delete
     * @throws IOException not a directory, directory not empty, etc.
     */
    private void rmdirWork(URI uri, BaseEntry lastPart) throws IOException {

        //  By this point, must be a directory to proceed.
        if (lastPart instanceof DirectoryEntry d) {

            //  Root directory cannot be deleted.
            if (d.isRoot()) {
                throw new AccessDeniedException("%s: Root directory cannot be deleted".formatted(uri));
            }

            //  Only empty directories are deleted/removed.
            DirectoryEntry entry = repository.getParentWithChildren(uri, lastPart.getId(), 0, 2);
            if (entry == null ||
                ((entry.getFiles() == null || entry.getFiles().isEmpty()) &&
                    (entry.getSubdirs() == null || entry.getSubdirs().isEmpty()))) {
                repository.delete(uri, lastPart.getId());
            } else {
                throw new DirectoryNotEmptyException(uri.toString());
            }
        } else {
            throw new NotDirectoryException(uri.toString());
        }
    }

    /**
     * Updates entry for attributes in the basic view
     * @param entryToUpdate entry to update
     * @param attribute attribute name to modify
     * @param value new attribute values
     * @throws IOException
     */
    private void setAttributeBasic(BaseEntry entryToUpdate,
                                   String attribute,
                                   Object value) throws IOException {

        switch (attribute) {
            case BASIC_ATTRIBUTE_CREATE_TIME:
                if (value instanceof Instant i)
                    entryToUpdate.setCreated(i);
                else if (value instanceof Long l)
                    entryToUpdate.setCreated(Instant.ofEpochMilli(l));
                else
                    throw new IllegalArgumentException("Invalid attribute value for %s".formatted(attribute));
                break;
            case BASIC_ATTRIBUTE_LAST_ACCESS_TIME:
                if (value instanceof Instant i)
                    entryToUpdate.setLastAccessed(i);
                else if (value instanceof Long l)
                    entryToUpdate.setLastAccessed(Instant.ofEpochMilli(l));
                else
                    throw new IllegalArgumentException("Invalid attribute value for %s".formatted(attribute));
                break;
            case BASIC_ATTRIBUTE_LAST_MODIFIED_TIME:
                if (value instanceof Instant i)
                    entryToUpdate.setLastModified(i);
                else if (value instanceof Long l)
                    entryToUpdate.setLastModified(Instant.ofEpochMilli(l));
                else
                    throw new IllegalArgumentException("Invalid attribute value for %s".formatted(attribute));
                break;
            default:
                throw new IllegalArgumentException("Setting attribute not supported: %s".formatted(attribute));
        }
    }

    /**
     * Walk the Neo4J file system tree and apply the visitor to each node (file, directory, etc)
     * based on the event type
     * @param uri starting point in tree
     * @param visitor visitor to apply to each node
     */
    private void walkFileTree(URI uri, FileVisitor<Neo4jfsTreeWalker.NeofjfsWalkerEvent> visitor) throws IOException {
        checkUri(uri);

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
        }
    }

    /**
     * Walk the Neo4J file system tree and apply the visitor to each node (file, directory, etc)
     * based on the event type
     * @param uri starting point in tree
     * @param fileVisitorKey key for finding registered visitor
     */
    private void walkFileTree(URI uri, String fileVisitorKey) throws IOException {
        checkUri(uri);
        walkFileTree(uri, visitorMap.get(fileVisitorKey));
    }

    /**
     * Checked for requested copy option in options passed to initial call
     * @param requested the copy option requested
     * @param options variable list of options
     * @return true if found, false otherwise
     */
    private boolean checkForOption(CopyOption requested, CopyOption... options) {
        return options != null && Arrays.stream(options).anyMatch(requested::equals);
    }

    /**
     * Some visitors are components which register themselves for simplified usage (e.g., no {@code new XYZFileVisitor()}
     * @param key unique identifier for visitor
     * @param visitor visitor instance.
     */
    public void registerVisitor(final String key, final FileVisitor visitor) {
        visitorMap.put(key, visitor);
    }

    /**
     * Avoids circular references between FileService and DirectoryService by having FileService register itself.
     * @param fileService
     */
    public void registerFileService(final FileService fileService) {
        this.fileService = fileService;
    }
}
