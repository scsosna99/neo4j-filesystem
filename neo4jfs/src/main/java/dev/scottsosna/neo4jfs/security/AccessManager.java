package dev.scottsosna.neo4jfs.security;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;

import java.nio.file.AccessMode;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public interface AccessManager {
    /**
     * Checks access of entry against the AccessModes provided.
     * @param entry the file/directory/entry to check permissions
     * @param modes requested access modes
     * @return an empty array if all checks pass, otherwise the {@code AccessMode}s that failed.
     */
    AccessMode[] checkAccess(BaseEntry entry, AccessMode... modes);

    /**
     * Convert set of {@link PosixFilePermission} to the form required by {@code AccessManager} implementation.
     * @param posixPermissions Posix permissions to convert
     * @return String representation of permissions stored with entry.
     */
    String convertPermissions(final Set<PosixFilePermission> posixPermissions);

    /**
     * Convert human-readable string to set of {@link PosixFilePermission}
     * @param permissions permission string, most likely for a file/directory/entry.
     * @return corresponding set of Posix permissions.
     */
    Set<PosixFilePermission> convertPermissions(final String permissions);

    /**
     * @return the admin group for the AccessManager implementation.
     */
    String getAdminGroup();

    /**
     * @return the admin user for the AccessManager implementation.
     */
    String getAdminUser();

    /**
     * Is currently-authenticated user considered admin or super-user?
     * @return true if admin; false otherwise
     */
    boolean isAdminUser();

    /**
     * @return root directory permissions used when creating file system.
     */
    String rootPermissions();

    /**
     * Validate the permission string against the {@code AccessManager} expected form.
     * @param permissions the permissions string to be validated
     * @return true if valid, false otherwise.
     */
    boolean validatePermissions(final String permissions);
}
