package dev.scottsosna.neo4jfs.service;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
import dev.scottsosna.neo4jfs.database.node.BaseEntry;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.*;

/**
 * Base class for all Neo4fjs services.
 */
public class BaseNeo4jfsService {

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
     */
    protected BaseNeo4jfsService() {
    }

    /**
     * Checks access of entry against the AccessModes provided without throwing exception
     * @param entry the entry being checked
     * @param modes the requested access modes for this file/directory/entry.
     * @return null if all checks pass, otherwise the AccessMode that failed.
     */
    protected AccessMode checkAccessNoThrows(final BaseEntry entry,
                                             final AccessMode... modes) {

        //  No access modes requested, so no need to check.
        if (modes.length == 0) return null;

        //  Attempt to get the authenticated user.
        Authentication authentication;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        } else {
            authentication = unknownUnauthenticatedUser;
        }

        //  Admin has access to everything, not allowed to restrict.
        if (Neo4jfsConstants.NAME_ADMIN_USER.equals(authentication.getName())) {
            return null;
        }

        //  Authenticated user is the owner of the file/directory/entry.
        if (authentication.getPrincipal().equals(entry.getUserName()) &&
            checkAccessForOwnerOrGroupOrAll(entry.getPermissions().substring(0, 3), modes) == null) {
            //  Access checks succeeded
            return null;
        }

        //  Authenticated user has an "authority" that matches the owning group of the file/directory/entry.
        var match = authentication.getAuthorities()
            .stream()
            .filter(a -> a.getAuthority().equals(entry.getGroupName()))
            .findFirst();
        if (match.isPresent() && checkAccessForOwnerOrGroupOrAll(entry.getPermissions().substring(3, 6), modes) == null) {
            //  Access checks succeeded.
            return null;
        }

        //  Nothing else, check everyone access.  {@code checkAccessWork} returns the mode not found
        //  when checking permissions which can be used for better exception/error handling.
        AccessMode toReturn = checkAccessForOwnerOrGroupOrAll(entry.getPermissions().substring(6), modes);
        return toReturn;
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

        AccessMode result = checkAccessNoThrows(entry, modes);
        if (result != null) {
            switch (result) {
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

        //  Attempt to get the authenticated user and confirm its the admin
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (Neo4jfsConstants.NAME_ADMIN_USER.equals(authentication.getName())) {
                return;
            }
        }

        throw new AccessDeniedException("Admin-only operation");
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

    /**
     * Compare the access modes requested against the permissions provided.  The permissions are the specific subset
     * being checked: first 3 characters for owning user, second 3 characters for owning group, third 3 characters for
     * everyone else.  Basically implementing Posix security flags for each entry/file/directory.
     * @param permsSubset 3-character permission flags for the entry
     * @param modes {@code AccessMode}s requested
     * @return null if all checks pass, otherwise the AccessMode that failed.
     */
    private AccessMode checkAccessForOwnerOrGroupOrAll(final String permsSubset, final AccessMode... modes) {
        //  Simple case: no permissions provided.
        if (permsSubset == null || (modes.length > 0 && permsSubset.equals(NEO4JFS_PERMISSION_NONE_GROUP))) return null;

        //  For each AccessMode checked, ensure that the correct Posix-like flag is enabled.
        char[] array = permsSubset.toCharArray();
        for (AccessMode mode: modes) {
            switch (mode) {
                case READ:
                    if (array[0] != NEO4JFS_PERMISSION_READ) return AccessMode.READ;
                    break;
                case WRITE:
                    if (array[1] != NEO4JFS_PERMISSION_WRITE) return AccessMode.WRITE;
                    break;
                case EXECUTE:
                    if (array[2] != NEO4JFS_PERMISSION_EXECUTE) return AccessMode.EXECUTE;
                    break;
            }
        }

        return null;
    }
}
