/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Licensed under the MIT license for non-commercial use.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 *
 * Licensed under the GPLv3 license for commercial use.  Please refer to LICENSE-GPL.md or
 * https://www.gnu.org/licenses/gpl-3.0.html for terms and conditions.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * expressed or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.database.node.DirectoryBuilder;
import dev.scottsosna.neo4jfs.database.node.DirectoryEntry;
import dev.scottsosna.neo4jfs.database.node.FileEntry;
import dev.scottsosna.neo4jfs.database.repository.DirectoryEntryRepository;
import dev.scottsosna.neo4jfs.database.repository.util.DumpTreeVisitor;
import dev.scottsosna.neo4jfs.database.repository.util.Neo4jfsTreeWalker;
import dev.scottsosna.neo4jfs.exception.Neo4jfsIdenticalSourceTargetException;
import dev.scottsosna.neo4jfs.exception.Neo4jfsUnknownEntryException;
import dev.scottsosna.neo4jfs.filesystem.Neo4jfsPath;
import dev.scottsosna.neo4jfs.filesystem.attribute.BasicFileAttributeViewImpl;
import dev.scottsosna.neo4jfs.filesystem.attribute.BasicFileAttributesImpl;
import dev.scottsosna.neo4jfs.filesystem.attribute.FileOwnerAttributeViewImpl;
import dev.scottsosna.neo4jfs.filesystem.attribute.PosixFileAttributeViewImpl;
import dev.scottsosna.neo4jfs.filesystem.option.Neo4jfsCopyOption;
import dev.scottsosna.neo4jfs.filesystem.option.Neo4jfsDeleteOption;
import dev.scottsosna.neo4jfs.security.AccessManager;
import dev.scottsosna.neo4jfs.service.util.CopyMoveConsumer;
import dev.scottsosna.neo4jfs.service.util.FileStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.*;

@Service
public class DirectoryServiceImpl extends BaseNeo4jfsService implements DirectoryService {

    private final DirectoryEntryRepository repository;
    private FileService fileService;
    private final Map<String, FileVisitor> visitorMap = new HashMap<>();

    /**
     * Logger for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(DirectoryServiceImpl.class);

    /**
     * Constructor
     * @param repository database repository for managing Directory nodes in Neo4J.
     * @param accessManager checks access permissions for service
     */
    public DirectoryServiceImpl(final DirectoryEntryRepository repository,
                                final AccessManager accessManager) {
        super(accessManager);
        this.repository = repository;
    }

    /**
     * Checks the existence, and optionally the accessibility, of a file or directory.
     * @param path the path to the file/directory to check
     * @param modes the access modes to check; may have zero elements.
     * @throws IOException if access or I/O error occurs
     */
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        checkUri(path.toUri());

        //  Since URI was checked/validated, path should almost certainly be a Neo4jfsPath.
        if (path instanceof Neo4jfsPath np) {
            //  Retrieve the nodes (entries) of the provided path.
            List<BaseEntry> entries = find(np.toUri());

            //  Null or empty set indicates path does not exist.
            if (entries == null || entries.isEmpty()) {
                throw new NoSuchFileException(path.toString());
            }

            //  Have entry, NOW we can actually check access.
            checkAccess(entries.getLast(), modes);
        } else {
            //  Should never happen, but just in case.
            throw new IllegalArgumentException("Path is not a Neo4jfsPath: %s".formatted(path));
        }
    }

    /**
     * Each file system needs a root '/' directory that is (somewhat) immutable
     * @param fsUri Neo4Jfs URI for the file system (partition)
     * @return the newly-created root directory
     * @throws IOException if I/O error occurs or non-admin attempting
     */
    public DirectoryEntry createRoot (final URI fsUri) throws IOException {
        checkAccessAdmin();
        checkUri(fsUri);
        return repository.createRoot(fsUri, accessManager.getAdminUser(), accessManager.getAdminGroup());
    }

    /**
     * Create file system root '/' directory if one doesn't already exist
     * @param fsUri Neo4Jfs URI for the file system (partition)
     * @return the root directory
     * @throws IOException if access or I/O error occurs
     */
    public DirectoryEntry findOrCreateRoot(final URI fsUri) throws IOException {
        checkUri(fsUri);
        DirectoryEntry d = repository.findRoot(fsUri);
        if (d == null) {
            d = createRoot(fsUri);
        }

        return d;
    }

    /**
     * Create new Neo4Jfs directory
     * @param uri fully-qualified Neo4Jfs URI specifying subdirectory to create
     * @return the newly created directory
     * @throws IOException I/O problem, such as parent directory doesn't exist.
     */
    public DirectoryEntry mkdir (final URI uri) throws IOException {
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
            //  Confirm write access to parent directory.
            checkAccess(dir, AccessMode.READ, AccessMode.WRITE);

            //  Make sure the name requested doesn't already exist
            BaseEntry child = repository.findNamedChild(uri, dir.getId(), path.getFileName().toString());
            if (child != null) {
                throw new FileAlreadyExistsException(path.toString());
            }

            //  Build a new directory node and persist it.
            DirectoryEntry newbie = new DirectoryBuilder(dir)
                .name(path.getFileName().toString())
                .root(false)
                .build();
            return repository.create(uri, newbie, dir);
        } else {
            //  In fact, parent is not a directory, fail.
            throw new NotDirectoryException(parent.toString());
        }
    }

    /**
     * Returns {@code BaseEntry} nodes representing path from root to specific directory/file.
     * @param uri fully-qualified Neo4Jfs URI specifying directory/file to find
     * @return {@code BaseEntry} list representing the target path or empty list if path doesn't exist.
     */
    public List<BaseEntry> find(final URI uri) {
        checkUri(uri);
        List<BaseEntry> entries = repository.find(uri, Path.of(uri));
        if (entries != null && !entries.isEmpty() && checkAccessNoThrows(entries.getLast(), AccessMode.READ) == null) {
            return entries;
        } else {
            return List.of();
        }
    }

    /**
     * Return a directory with a paginated list of children (files, subdirectories)
     * @param fsUri Neo4Jfs base URI
     * @param parent specific directory for which children are returned
     * @param skip pagination: how many children skipped
     * @param limit pagination: how many children returned
     * @return list of BaseEntry for the children or an empty list.
     */
    public List<BaseEntry> findChildren(final URI fsUri,
                                        final DirectoryEntry parent,
                                        final int skip,
                                        final int limit) {
        checkUri(fsUri);

        //  Retrieve children from Neo4J but only return those which user has READ access.
        return repository.getChildren(fsUri, parent, skip, limit)
            .stream()
            .filter(e -> checkAccessNoThrows(e, AccessMode.READ) != null)
            .toList();
    }

    /**
     * Return a list of all children entries (files, subdirectories, etc) for the directory specified.
     * @param fsUri Neo4Jfs base URI
     * @param parent specific directory for which children are returned
     * @param skip pagination: how many children skipped
     * @param limit pagination: how many children returned
     * @return list of DirectoryEntry or an empty list.
     */
    public List<DirectoryEntry> findSubdirs(final URI fsUri,
                                            final DirectoryEntry parent,
                                            final int skip,
                                            final int limit) {
        checkUri(fsUri);

        //  Retrieve subdirectories from Neo4J but only return those which user has READ access.
        return repository.getSubdirs(fsUri, parent, skip, limit)
            .stream()
            .filter(e -> checkAccessNoThrows(e, AccessMode.READ) != null)
            .toList();
    }

    /**
     * Copy file or directory to new location
     * @param sourceUri source file or directory to copy
     * @param fsUri target location
     * @param options copy options
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void copy(URI sourceUri, URI fsUri, CopyOption... options) throws IOException {
        //  Prologue method does initial checks/validation before delegating to method to do actual work.
        prologueCopyMove(sourceUri, fsUri, this::copyWork, options);
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
     * Return the parent directory of the specified URI
     * @param uri fully-qualified Neo4Jfs URI specifying directory/file to find parent of
     * @return pagent directory
     * @throws IOException thrown when user doesn't have access
     */
    public BaseEntry parent(final URI uri) throws IOException {
        checkUri(uri);
        BaseEntry parent = repository.parent(uri);
        checkAccess(parent, AccessMode.READ);
        return parent;
    }

    /**
     * Delete node specified by URI, file or directory
     * @param uri Neo4Jfs URI specifying either file or directory to delete.
     * @param options options specifying how to delete
     * @throws IOException I/O errors such file/directory doesn't exist or directory not empty.
     */
    public void delete(final URI uri, final Neo4jfsDeleteOption... options) throws IOException {

        //  Get the requested pathname as ordered list of nodes.
        List<BaseEntry> parts = prologue(uri);

        //  Last part must be a directory.
        BaseEntry lastPart = parts.getLast();
        switch (lastPart) {
            case FileEntry f:
                fileService.delete(uri);
                break;
            case DirectoryEntry d:
                if (checkForOption(Neo4jfsDeleteOption.DELETE_RECURSIVELY, options, Neo4jfsDeleteOption.class)) {
                    FileSystemUtils.deleteRecursively(Path.of(uri));
                } else {
                    rmdirWork(uri, lastPart);
                }
                break;
            default:
                throw new Neo4jfsUnknownEntryException(uri, lastPart.getClass().getName());
        }
    }

    /**
     * Check whether directory/file specified exists,
     * @param uri Neo4Jfs URI for the directory or file to check
     * @return true if exists, false otherwise.
     */
    public boolean exists(final URI uri) {
        checkUri(uri);
        return repository.pathExists(uri);
    }

    /**
     * Add persisted file (FileEntry node) to its parent directory (DirectoryEntry node)
     * @param fsUri Neo4Jfs file system URI
     * @param directory parent/containing directory of the file to add
     * @param fileToAdd file to add
     * @return updated DirectoryEntry
     */
    public DirectoryEntry addFile(final URI fsUri,
                                  final DirectoryEntry directory,
                                  final FileEntry fileToAdd) {
        checkUri(fsUri);
        directory.setFiles(List.of(fileToAdd));
        return repository.save(fsUri, directory);
    }

    /**
`     * Return a directory with a paginated list of subdirectories
     * @param fsUri Neo4Jfs base URI
     * @param parent directory for which filess are returned
     * @param skip pagination: how many subdirs skipped
     * @param limit pagination: how many subdirs returned
     * @return updated {@code DirectoryEntry} with children collections
     */
    public List<FileEntry> findFiles(final URI fsUri,
                                     final DirectoryEntry parent,
                                     final int skip,
                                     final int limit) {
        checkUri(fsUri);
        return repository.getFiles(fsUri, parent, skip, limit);
    }

    /**
     * Returns the entry specified by URI as BasicFileAttributeView, needed by file system provider.
     * @param uri Neo4Jfs URI for the directory or file to read attribute view for.
     * @param clazz type of view to return.
     * @param options ignored
     * @return the attributes as a "view"
     * @throws IOException I/O error occurred while retrieving the entry to return
     */
    public <T extends FileAttributeView> FileAttributeView readAttributeView(final URI uri,
                                                                             final Class<T> clazz,
                                                                             final LinkOption... options) throws IOException {
        checkUri(uri);

        //  Always retrieve the path first to immediately notify if file/directory URI is invalid.
        List<BaseEntry> pathEntries = find(uri);
        if (pathEntries == null || pathEntries.isEmpty()) {
            throw new NoSuchFileException("%s: no such file or directory".formatted(uri));
        }

        //  Check permissions
        BaseEntry target = pathEntries.getLast();
        checkAccess(target, AccessMode.READ);
        
        //  Work done is dependent on the view requested.
        if (clazz == BasicFileAttributeView.class) {
            //  BaseEntry implements BasicFileAttributeView.
            return new BasicFileAttributeViewImpl(target);
        } else if (clazz == FileOwnerAttributeView.class) {
            return new FileOwnerAttributeViewImpl(target);
        } else if (clazz == PosixFileAttributeView.class) {
            return new PosixFileAttributeViewImpl(target);
        } else {
            throw new IllegalArgumentException("Requested view not supported: %s".formatted(clazz.getName()));
        }
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
    public void setAttribute(final URI uri,
                             final String viewName,
                             final String attribute,
                             final Object value,
                             final LinkOption... options) throws IOException {
        checkUri(uri);

        //  Get entry and check user's access to ensure user permitted to update/set attributes.
        BaseEntry entry = find(uri).getLast();
        checkAccess(entry, AccessMode.READ, AccessMode.WRITE);

        switch (viewName) {
            case BasicFileAttributeViewImpl.VIEW_NAME:
                setAttributeBasic(entry, attribute, value);
                break;
            case PosixFileAttributeViewImpl.VIEW_NAME:
                setAttributePosix(entry, attribute, value);
                break;
            default:
                //  Generally view name has already been validated, but just in case.
                throw new IllegalArgumentException("Setting attribute not supported for view: %s".formatted(attribute));
        }

        //  Once here we know something modified as otherwise an exception is thrown.
        repository.save(uri, entry, BaseEntry.class);
    }

    /**
     * Walks tree, dumping file structure to logger
     * @param uri Neo4Jfs URI for the directory to dump
     * @throws IOException I/O error occurred while walking tree
     */
    public void dumpTree(URI uri) throws IOException {
        checkUri(uri);
        Files.walkFileTree(Path.of(uri), new DumpTreeVisitor());
//        walkFileTree(uri, new DumpTreeVisitor());
    }

    /**
     * Avoids circular references between FileService and DirectoryService by having FileService register itself.
     * @param fs file service instance
     */
    public void registerFileService(final FileService fs) {
        this.fileService = fs;
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
     * Determine if existing file/directory can be replaced during copy/merge
     * @param fsUri base Neo4Jfs file system URI
     * @param target target file/directory of move copy or move
     * @param options copy options supplied by initial caller
     * @throws FileAlreadyExistsException target entry exists and cannot be overwritten.
     */
    private void checkForExisting(final URI fsUri,
                                  final BaseEntry target,
                                  final CopyOption... options) throws FileAlreadyExistsException {
        //  Target file exists therefore check whether overwriting allowed.
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

    }

    /**
     * Handles the workflow required for a copy, handling differences between files, directories, options, etc.
     * @param sourceUri Neo4Jfs URI for source file or directory
     * @param source {@code BaseEntry} representing source file or directory
     * @param sourceParent parent directory of file or directory to copy
     * @param targetUri Neo4Jfs URI for target location of copy
     * @param target {@code BaseEntry} representing target file or directory, which may be null.
     * @param targetParent parent directory of copied file or directory, which must exist.
     * @param targetName name to use for copied file or directory.
     * @param options copy options supplied by initial caller.
     * @throws IOException if I/O error occurs
     */
    private void copyWork(final URI sourceUri,
                          final BaseEntry source,
                          final DirectoryEntry sourceParent,
                          final URI targetUri,
                          final BaseEntry target,
                          final DirectoryEntry targetParent,
                          final String targetName,
                          final CopyOption[] options) throws IOException {

        switch(source) {
            case FileEntry f:
                //  Can existing target be replaced?
                checkForExisting(sourceUri, target, options);

                //  Copy file to target location.
                fileService.copy(f, targetUri, targetParent, options);
                break;
            case DirectoryEntry d:
                if (checkForCopyOption(Neo4jfsCopyOption.COPY_RECURSIVELY, options)) {
                    //  Deep copy directory to target location.
                    FileSystemUtils.copyRecursively(Path.of(sourceUri), Path.of(targetUri));
                } else {
                    //  Only copy files from current directory to new.
                    Iterator<FileEntry> it = new FileStream(sourceUri, d).iterator();
                    while (it.hasNext()) {
                        FileEntry file = it.next();
                        fileService.copy(file, targetUri, targetParent, options);
                    }
                }
                break;
            default:
                throw new Neo4jfsUnknownEntryException(source.getClass().getName());
        }
    }

    /**
     Handles the workflow required for a move, handling differences between files, directories, options, etc.
     * @param source source file or directory to move
     * @param sourceParent source parent directory
     * @param target target of move, which may be null
     * @param targetParent target parent directory, which always exists
     * @param targetName name of target, may require renaming exsisting entry.
     * @param options "copy" options to apply to move
     * @throws IOException if an I/O error occurs.
     */
    private void moveWork(final URI sourceUri,
                          final BaseEntry source,
                          final DirectoryEntry sourceParent,
                          final URI targetUri,
                          final BaseEntry target,
                          final DirectoryEntry targetParent,
                          final String targetName,
                          final CopyOption[] options) throws IOException {

        //  Can existing target be replaced?
        checkForExisting(targetUri, target, options);

        if (sourceParent.equals(targetParent)) {
            //  Source and parent directories the same, so only need to rename and save.
            if (!source.getName().equals(targetName)) {
                source.setName(targetName);
                repository.save(sourceUri, source, BaseEntry.class);
            } else {
                //  Hmmm, name didn' change, no work to do.
                logger.debug("Name unchanged, skipping rename");
            }

            return;
        }

        //  Steps are identical except for relationship which differs for directories vs. files.
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

        //  First delete the relationship and then save the new relationship in the same transaction
        repository.save(sourceUri, List.of(
            (Callable<Integer>) () -> repository.deleteRelationship(sourceUri, sourceParent.getId(), source.getId()),
            (Callable<BaseEntry>) () -> repository.save(targetUri, targetParent))
        );
    }

    /**
     * Initial steps for multiple options, ensuring correct URI and file/directory specified exists
     * @param uri Neo4Jfs URI for the directory path on which to operate
     * @return the nodes representing the path.
     * @throws IOException if the path doesn't exist.
     */
    private List<BaseEntry> prologue(final URI uri) throws IOException{
        checkUri(uri);

        //  Confirm path existence.
        List<BaseEntry> pathParts = find(uri);
        if (pathParts.isEmpty()) {
            throw new NoSuchFileException("%s: no such file or directory".formatted(uri));
        }

        return pathParts;
    }

    /**
     * Initial steps executed for both copy and move operations.
     * @param sourceUri source file or directory
     * @param targetUri target file of directory
     * @param options copy options to apply (to either)
     * @throws IOException if an I/O error occurs.
     */
    private void prologueCopyMove(URI sourceUri,
                                  URI targetUri,
                                  final CopyMoveConsumer workMethod,
                                  final CopyOption... options) throws IOException {

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
            targetUri,
            targetEntry,
            targetParentEntry,
            targetName,
            options);
    }

    /**
     * Actual steps/guts for removing an existing directory.
     * @param uri file system's URI by specifying the partition
     * @param shouldBeDirectory directory to delete
     * @throws IOException I/O error such as not a directory, directory not empty, etc.
     */
    private void rmdirWork(final URI uri, final BaseEntry shouldBeDirectory) throws IOException {

        //  By this point, must be a directory to proceed.
        if (shouldBeDirectory instanceof DirectoryEntry d) {

            //  Check access permissions, READ access for checking files or subdirectories in to-be-deleted directory.
            checkAccess(d, AccessMode.READ, AccessMode.WRITE);

            //  Root directory cannot be deleted.
            if (d.isRoot()) {
                throw new AccessDeniedException("%s: Root directory cannot be deleted".formatted(uri));
            }

            //  Only empty directories are deleted/removed.
            List<BaseEntry> children = repository.getChildren(uri, d, 0, 2);
            if (children == null || children.isEmpty()) {
                repository.delete(uri, shouldBeDirectory.getId());
            } else {
                throw new DirectoryNotEmptyException(uri.toString());
            }
        } else {
            throw new NotDirectoryException(uri.toString());
        }
    }

    /**
     * Updates entry for "basic" view attributes
     * @param entryToUpdate entry to update
     * @param attribute attribute name to modify
     * @param value new attribute value
     * @throws IOException if an I/O error occurs, such as invalid attribute value.
     */
    private void setAttributeBasic(final BaseEntry entryToUpdate,
                                   final String attribute,
                                   final Object value) throws IOException {

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
     * Updates entry for "basic" view attributes
     * @param entryToUpdate entry to update
     * @param attribute attribute name to modify
     * @param value new attribute value
     * @throws IOException if an I/O error occurs, such as invalid attribute value.
     */
    private void setAttributePosix(final BaseEntry entryToUpdate,
                                   final String attribute,
                                   final Object value) throws IOException {

        switch (attribute) {
            case POSIX_ATTRIBUTE_GROUP:
                if (value instanceof String s) {
                    entryToUpdate.setOwnerGroupName(s);
                } else {
                    throw new IllegalArgumentException("Invalid attribute value for %s".formatted(attribute));
                }
                break;
            case POSIX_ATTRIBUTE_OWNER:
                if (value instanceof String s) {
                    entryToUpdate.setOwnerUserName(s);
                } else {
                    throw new IllegalArgumentException("Invalid attribute value for %s".formatted(attribute));
                }
                break;
            default:
                throw new IllegalArgumentException("Setting attribute not supported: %s".formatted(attribute));
        }
    }

    /**
     * Walks the Neo4Jfs tree and applies the visitor to each entry (file, directory, etc), based on the event type
     * @param uri starting point in tree
     * @param visitor visitor to apply to each node
     */
    private void walkFileTree(final URI uri,
                              final FileVisitor<Neo4jfsTreeWalker.NeofjfsWalkerEvent> visitor) throws IOException {

        var attribs = new BasicFileAttributesImpl(null);
        try (var walker = new Neo4jfsTreeWalker()) {
            var env = walker.walk(uri);
            do {
                switch (env.eventType()) {
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
            } while (env.eventType() != Neo4jfsTreeWalker.EventType.FINISHED);
        }
    }

    /**
     * Retrieves a visitor specified by name and then walks the Neo4Jfs tree using that visitor.
     * @param uri starting point in tree
     * @param fileVisitorKey key for finding registered visitor
     */
    private void walkFileTree(final URI uri, final String fileVisitorKey) throws IOException {
        var visitor = visitorMap.get(fileVisitorKey);
        if (visitor == null) {
            throw new IllegalArgumentException("No visitor registered for key: %s".formatted(fileVisitorKey));
        }
        walkFileTree(uri, visitorMap.get(fileVisitorKey));
    }
}
