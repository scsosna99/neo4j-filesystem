package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.database.repository.DirectoryEntryRepository;
import dev.scottsosna.neo4jfs.database.repository.util.DebuggingFileVisitor;
import dev.scottsosna.neo4jfs.database.repository.util.DirectoryDeleteFileVisitor;
import dev.scottsosna.neo4jfs.database.repository.util.Neo4jfsFileAttributes;
import dev.scottsosna.neo4jfs.database.repository.util.Neo4jfsTreeWalker;
import dev.scottsosna.neo4jfs.exception.Neo4jfsIdenticalSourceTargetException;
import dev.scottsosna.neo4jfs.exception.Neo4jfsUnknownEntryException;
import dev.scottsosna.neo4jfs.service.util.CopyMoveConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final static Logger logger = LoggerFactory.getLogger(DirectoryServiceImpl.class);

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
            //  In fact, parent is not a directory, fail.
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
     * Copy file or directory to new location
     * @param sourceUri source file or directory to copy
     * @param targetUri target location
     * @param options copy options
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void copy(URI sourceUri, URI targetUri, CopyOption... options) throws IOException {
        //  Prologue method does initial checks/validation before delegating to method to do actual work.
        prologueCopyMove(sourceUri, targetUri, this::copyWork, options);
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
     * Move file and directory within same file system.
     * @param sourceUri file or directory to move
     * @param targetUri target location.
     * @param options  options specifying how the move should be done
     * @throws IOException problems executing the move.
     */
    @Override
    public void move(URI sourceUri, URI targetUri, CopyOption... options) throws IOException {
        //  Prologue method does initial checks/validation before delegating to actual worker method.
        prologueCopyMove(sourceUri, targetUri, this::moveWork, options);
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

    public DirectoryEntry findSubdirs(final URI uri,
                                      final String parentId,
                                      final int skip,
                                      final int limit) {
        checkUri(uri);
        return repository.getSubdirs(uri, parentId, skip, limit);
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


    private void copyWork(final URI fsUri,
                          final BaseEntry source,
                          final DirectoryEntry sourceParent,
                          final BaseEntry target,
                          final DirectoryEntry targetParent,
                          final String targetName,
                          final CopyOption[] options) throws IOException {
        switch(source) {
            case FileEntry f:
                // if target exists and is a file, delete and copy into parent directory.
                // if target exists and is a directory, copy into target directory.
                // if target does not exist, copy into parent directory
                break;
            case DirectoryEntry d:
                // if target exists and is a directory, recursively copy into directory
                // if target does not exists, recursively copy into target parent
                break;
            default:
                throw new Neo4jfsUnknownEntryException(source.getClass().getName());
        }
    }


    /**
     * Do actual moving of source to target, whatever that may be.
     *
     * @param fsUri Neo4Jfs file system URI
     * @param source source file or directory to move
     * @param sourceParent source parent directory
     * @param target target of maove, which may be null
     * @param targetParent target parent directory, which always exists
     * @param targetName name of target, may require renaming exsisting entry.
     * @param options "copy" options to apply to move
     * @throws IOException if an I/O error occurs.
     */
    private void moveWork(final URI fsUri,
                          final BaseEntry source,
                          final DirectoryEntry sourceParent,
                          final BaseEntry target,
                          final DirectoryEntry targetParent,
                          final String targetName,
                          final CopyOption[] options) throws IOException {

        if (target != null) {
            //  Target exists, move can only proceed if explicitly alloweing target to be replaced,
            //  which means deleting existing target to be replaced by source.
            if (checkForCopyOption(StandardCopyOption.REPLACE_EXISTING, options)) {
                //  Target exists and REPLACE_EXISTING specified, therefore target must be deleted
                repository.delete(fsUri, target.getId());
            } else {
                //  Overwriting not allowed, therefore cannot complete move.
                throw new FileAlreadyExistsException(target.toString());
            }
        }

        if (sourceParent.equals(targetParent)) {
            //  Source and parent directories the same, so only need to rename and save.
            if (!source.getName().equals(targetName)) {
                source.setName(targetName);
                repository.save(fsUri, source, BaseEntry.class);
            } else {
                //  Hmmm, name didn' change, no work to do.
                logger.debug("Name unchanged, skipping rename");
            }

            return;
        }

        //  Steps are identical except for relationship which differs for directories vs. files.
        repository.deleteRelationship(fsUri, sourceParent.getId(), source.getId());
        source.setName(targetName);
        switch (source) {
            case FileEntry f:
                targetParent.setFiles(List.of(f));
                break;
            case DirectoryEntry d:
                targetParent.setSubdirs(List.of(d));
                break;
            default:
                //  Totally unexpected.
                throw new Neo4jfsUnknownEntryException(source.getClass().getName());
        }
        repository.save(fsUri, targetParent);
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

    /**
     * Initial steps required before getting into guts of a copy or move operation.
     * @param sourceUri source file or directory
     * @param targetUri target file of directory
     * @param options copy options to apply (to either)
     * @return target path.
     * @throws IOException if an I/O error occurs.
     */
    private void prologueCopyMove(URI sourceUri,
                                  URI targetUri,
                                  CopyMoveConsumer workMethod,
                                  CopyOption... options) throws IOException {

        //  Check scheme and normalize URIs.
        sourceUri = checkUri(sourceUri);
        targetUri = checkUri(targetUri);

        //  Root can never be copied/moved.
        if (sourceUri.getPath().isEmpty()) {
            throw new UnsupportedOperationException("Root directory cannot be copied/moved:");
        }

        //  Source and target must not refer to same location.
        if (sourceUri.equals(targetUri)) {
            throw new Neo4jfsIdenticalSourceTargetException(sourceUri.toString());
        }

        //  Retrieve source, ensure existence, get needed entries.
        List<BaseEntry> sourceEntries = prologue(sourceUri);
        BaseEntry sourceEntry = sourceEntries.getLast();
        DirectoryEntry sourceParentEntry = (DirectoryEntry) sourceEntries.get(sourceEntries.size() - 2);


        //  Little more tricky to determine target ... may exist, may not, may be overriden, may not, etc., etc.
        BaseEntry targetEntry = null;
        DirectoryEntry targetParentEntry = null;
        Path targetPath = Path.of(targetUri);
        List<BaseEntry> targetEntries = repository.find(targetUri, targetPath, true);
        String targetName = null;
        if (targetEntries.size() > targetPath.getNameCount()) {
            BaseEntry targetLast = targetEntries.getLast();

            //  When path entries returned is greater than count from target path - the target entries include root while
            //  name count doesn't - then two situations exist:
            switch (targetLast) {
                case FileEntry fe:
                    //  Last entry of target is a file which _may_ be deleted/replaced if correct
                    //  CopyOption provided.
                    targetEntry = targetEntries.getLast();
                    targetParentEntry = (DirectoryEntry) targetEntries.get(targetEntries.size() - 2);
                    targetName = targetEntry.getName();
                    break;
                case DirectoryEntry de:
                    //  When last entry is directory, then whatever is being copied/moved is put
                    //  into target directory.
                    targetParentEntry = de;
                    targetName = sourceEntry.getName();
                    break;
                default:
                    //  Should never happen but ....
                    throw new Neo4jfsUnknownEntryException(targetLast.toString());
            }
        } else {
            //  When number of target entries is equal to name count of target path, the target doesn't
            //  exist yet, therefore last elemenet NUST be a directory.
            if (targetEntries.isEmpty() || !(targetEntries.getLast() instanceof DirectoryEntry)) {
                throw new NotDirectoryException("%s: Not a directory".formatted(targetUri));
            }
            targetParentEntry = (DirectoryEntry) targetEntries.getLast();
            targetName = targetPath.getFileName().toString();
        }

        //  Attempt to do actual move/copy by delegating to method.
        workMethod.apply(sourceUri,
            sourceEntry,
            sourceParentEntry,
            targetEntry,
            targetParentEntry,
            targetName,
            options);
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
    private boolean checkForCopyOption(CopyOption requested, CopyOption[] options) {
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
