package dev.scottsosna.neo4jfs.filesystem.attribute;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

/**
 * Simple implementation of PosixFileAttributes.
 */
public class PosixFileAttributesImpl extends BasicFileAttributesImpl implements PosixFileAttributes {

    /**
     * Constructor
     * @param entry file/directory providing the attributes.
     */
    public PosixFileAttributesImpl(BaseEntry entry) {
        super(entry);
    }

    /**
     * @return owner of the file/directory
     */
    @Override
    public UserPrincipal owner() {
        return new UserPrincipalImpl(entry.getUserName());
    }

    /**
     * @return owning group of the file/directory
     */
    @Override
    public GroupPrincipal group() {
        return new GroupPrincipalImpl(entry.getGroupName());
    }

    /**
     * Permissions for this file/directory as a set of PosixFilePermissions.
     * @return
     */
    @Override
    public Set<PosixFilePermission> permissions() {
        return entry.getPosixPermissions();
    }
}
