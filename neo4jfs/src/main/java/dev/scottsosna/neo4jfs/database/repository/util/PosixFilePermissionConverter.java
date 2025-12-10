package dev.scottsosna.neo4jfs.database.repository.util;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for transforming the {@code PosixFilePermission} enum used by Java NIO into a *nix-like permissions
 * string (e.g., output from ls -l command) and vice-versa.
 */
public class PosixFilePermissionConverter {

    /**
     * Neo4Jfs permissions are stored as the 9-character string displayed by a Unix "ls -l" command.  This static
     * pattern can be used to validate that the permissions string is correctly formatted to prevent errors.
     */
    private static Pattern validation = Pattern.compile("^[r-][w-][x-][r-][w-][x-][r-][w-][x-]$");

    /**
     * Convert set of {@link PosixFilePermission} to a (somewhat) human-readable string as displayed by *nix ls -l command
     * @param posixPermissions Posix permissions to convert
     * @return String representation of permissions stored with entry.
     */
    public static String convert(final Set<PosixFilePermission> posixPermissions) {
        char[] toReturn = new char[] {'-','-','-','-','-','-','-','-','-',};
        for (PosixFilePermission one: posixPermissions) {
            switch (one) {
                case OWNER_READ: toReturn[0] = 'r'; break;
                case OWNER_WRITE: toReturn[1] = 'w'; break;
                case OWNER_EXECUTE: toReturn[2] = 'x'; break;
                case GROUP_READ: toReturn[3] = 'r'; break;
                case GROUP_WRITE: toReturn[4] = 'w'; break;
                case GROUP_EXECUTE: toReturn[5] = 'x'; break;
                case OTHERS_READ: toReturn[6] = 'r'; break;
                case OTHERS_WRITE: toReturn[7] = 'w'; break;
                case OTHERS_EXECUTE: toReturn[8] = 'x'; break;
            }
        }

        return new String(toReturn);
    }

    /**
     * Convert human-readable string to set of {@link PosixFilePermission}
     * @param permissions permission string, most likely for a file or directory.
     * @return Corresponding set of permissions.
     */
    public static Set<PosixFilePermission> convert(final String permissions) {
        if (permissions == null || permissions.isEmpty()) return EnumSet.noneOf(PosixFilePermission.class);

        //  Make sure the permissions string is valid before proceeding.
        if (!validate(permissions)) {
            throw new IllegalArgumentException("Permissions string must be 9 characters long");
        }

        Set<PosixFilePermission> toReturn = EnumSet.noneOf(PosixFilePermission.class);
        char[] array = permissions.toCharArray();
        if (array[0] == 'r') toReturn.add(PosixFilePermission.OWNER_READ);
        if (array[1] == 'w') toReturn.add(PosixFilePermission.OWNER_WRITE);
        if (array[2] == 'x') toReturn.add(PosixFilePermission.OWNER_EXECUTE);
        if (array[3] == 'r') toReturn.add(PosixFilePermission.GROUP_READ);
        if (array[4] == 'w') toReturn.add(PosixFilePermission.GROUP_WRITE);
        if (array[5] == 'x') toReturn.add(PosixFilePermission.GROUP_EXECUTE);
        if (array[6] == 'r') toReturn.add(PosixFilePermission.OTHERS_READ);
        if (array[7] == 'w') toReturn.add(PosixFilePermission.OTHERS_WRITE);
        if (array[8] == 'x') toReturn.add(PosixFilePermission.OTHERS_EXECUTE);

        return Collections.unmodifiableSet(toReturn);
    }

    /**
     * Validate the permission string against the regex.
     * @param permissions the permissions string to be validated
     * @return true if valid, false otherwise.
     */
    public static boolean validate(final String permissions) {
        return validation.matcher(permissions).matches();
    }
}
