package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import dev.scottsosna.neo4jfs.security.AccessManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.NEO4JFS_URI_SCHEME;

/**
 * Base class for all Neo4fjs services.
 */
public class BaseNeo4jfsService {

    /**
     * The AccessManager determines whether the user has the correct permissions to execute
     * an operation.  The access modes are NIO-defined POSIX access though the meaning of
     * how the permissions interpreted is implementation-specific.
     */
    protected AccessManager accessManager;

    /**
     *  Default user with presumably least permissions, used when no authenticated user is available via Spring security.
     */
    private static final Authentication unknownUnauthenticatedUser;
    static {
        unknownUnauthenticatedUser = new AnonymousAuthenticationToken(
            "anonymousUser",
            Neo4jfsConstants.NAME_UNAUTHENTICATED_USER,
            List.of(new SimpleGrantedAuthority(Neo4jfsConstants.NAME_UNAUTHENTICATED_GROUP)));
    }

    /**
     * Constructor protected to prevent direct instantiation.
     * @param accessManager checks access permissions for service
     */
    protected BaseNeo4jfsService(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    /**
     * Checks access of entry against the AccessModes provided without throwing exception
     * @param entry the entry being checked
     * @param modes the requested access modes for this file/directory/entry.
     * @return null if all checks pass, otherwise the AccessMode that failed.
     */
    protected AccessMode checkAccessNoThrows(final BaseEntry entry,
                                             final AccessMode... modes) {
        AccessMode[] failed = accessManager.checkAccess(entry, modes);
        return (failed == null || failed.length == 0) ? null : failed[0];
    }

    /**
     * Checks access of entry against the AccessModes provided.  Essentially this is {@code FileSystemProvider.checkAccess}
     * but using the entry directly when possible rather than re-retrieve.
     * @param entry the entry being checked
     * @param modes the requested access modes for this file/directory/entry.
     * @throws IOException if access is denied.
     */
    protected void checkAccess (final BaseEntry entry,
                                final AccessMode... modes) throws IOException {

        AccessMode[] failed = accessManager.checkAccess(entry, modes);
        if (failed != null && failed.length > 0) {
            switch (failed[0]) {
                case READ:
                    throw new NoSuchFileException(entry.getName());
                case WRITE:
                    throw new AccessDeniedException("Write access denied for %s.".formatted(entry.getName()));
                case EXECUTE:
                    throw new AccessDeniedException("Execute access denied for %s.".formatted(entry.getName()));
            }
        }
    }

    /**
     * Checking access for admin-only operations.
     * @throws AccessDeniedException thrown when security context is not available or principal isn't the admin user.
     */
    protected void checkAccessAdmin() throws AccessDeniedException {
        if (!accessManager.isAdminUser()) {
            throw new AccessDeniedException("Admin-only operation");
        }
    }

    /**
     * Checked for requested copy option in options passed to initial call
     * @param requested the copy option requested
     * @param options variable list of options
     * @return true if found, false otherwise
     */
    protected boolean checkForCopyOption(CopyOption requested, CopyOption[] options) {
        return checkForOption(requested, options, CopyOption.class);
    }

    /**
     * General method for checking for required enum in the list of enums (options) passed into a call.
     * @param required the value required/desired
     * @param options array of options passed in as varargs to a call
     * @param clazz the specific class/enum
     * @return true if the option exists, false otherwise
     */
    protected <T> boolean checkForOption(T required, T[] options, Class<T> clazz) {
        return
            options != null &&
            options.length > 0 &&
            clazz.isAssignableFrom(required.getClass()) &&
            Arrays.asList(options).contains(required);
    }

    /**
     * Check URI for structure and usability.     *
     * @param uri URI to validate.
     * @return normalized URI.
     */
    protected URI checkUri(final URI uri) {
        if (!NEO4JFS_URI_SCHEME.equals(uri.getScheme())) {
            throw new IllegalArgumentException("URI scheme must be " + NEO4JFS_URI_SCHEME + ".");
        }

        //  Empty path not allowed, must at least provide root "/"
        if (uri.getPath() == null || uri.getPath().isEmpty()) {
            throw new IllegalArgumentException("URI path required.");
        }

        //  Normalize URI in case anyone doing tricky navigation.
        return uri.normalize();
    }
}
