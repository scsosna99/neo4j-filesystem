/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.security;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.AccessMode;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Dummy access manager that always succeeds.  Primarily intended for development/testing purposes
 * but could be used in production if desired.
 */
@Service
@ConditionalOnProperty(prefix = "neo4jfs", name = "security", havingValue = "dummy")
public class DummyAccessManager implements AccessManager {

    /**
     * Required to have but immaterial with dummy security where access checks always pass.
     */
    @Value("${neo4jfs.security.dummy.rootPermissions:---------}")
    private String rootPermissions;

    /**
     * Complete set of Posix permissions blindly returned to anyone asking.
     */
    private static final Set<PosixFilePermission> ALL_PERMISSIONS = Set.of(
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.GROUP_WRITE,
        PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.OTHERS_READ,
        PosixFilePermission.OTHERS_WRITE,
        PosixFilePermission.OTHERS_EXECUTE
    );

    /**
     * "Checks" access of entry but always succeeds
     * @param entry the file/directory/entry to check permissions
     * @param modes requested access modes
     * @return empty array to emulate successful check.
     */
    @Override
    public AccessMode[] checkAccess(final BaseEntry entry, final AccessMode... modes) {
        return new AccessMode[0];
    }

    /**
     * Convert set of {@link PosixFilePermission} to the form required by {@code AccessManager} implementation.
     * @param posixPermissions Posix permissions to convert
     * @return String representation of permissions stored with entry.
     */
    public String convertPermissions(final Set<PosixFilePermission> posixPermissions) {
        return rootPermissions;
    }

    /**
     * Convert human-readable string to set of {@link PosixFilePermission}
     * @param permissions permission string, most likely for a file/directory/entry.
     * @return corresponding set of Posix permissions.
     */
    public Set<PosixFilePermission> convertPermissions(final String permissions) {
        return ALL_PERMISSIONS;
    }

    /**
     * @return the admin group for the AccessManager implementation.
     */
    public String getAdminGroup() {
        return "dummy";
    }

    /**
     * @return the admin user for the AccessManager implementation.
     */
    public String getAdminUser() {
        return "dummy";
    }

    /**
     * Is currently-authenticated user considered admin or super-user?
     * @return true if admin; false otherwise
     */
    public boolean isAdminUser() {
        return true;
    }

    /**
     * @return root directory permissions used when creating file system.
     */
    public String rootPermissions() {
        return rootPermissions;
    }

    /**
     * @return name of current authenticaated user
     */
    public String userName() {
        return "dummy";
    }


    /**
     * Validate the permission string against the regex.
     * @param permissions the permissions string to be validated
     * @return true if valid, false otherwise.
     */
    public boolean validatePermissions(final String permissions) {
        return true;
    }
}
