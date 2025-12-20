package dev.scottsosna.neo4jfs.database.node;

import dev.scottsosna.neo4jfs.database.repository.util.PosixFilePermissionConverter;
import lombok.*;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.id.UuidStrategy;

import java.net.URI;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Set;

/**
 * Basic information required of all Neo4Jfs entries (files, directories, etc.)
 */
@Getter @Setter @NoArgsConstructor @EqualsAndHashCode
public class BaseEntry {

    /**
     * Neo4J node identifier, auto-generated UUID.
     */
    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    String id;

    /**
     * Name of the entry
     */
    String name;

    /**
     * Username of creator, owner of the entry.
     */
    String userName;

    /**
     * Name of group associated with this entry, usually for security reasons.
     */
    String groupName;

    /**
     * Posix permissions in their typical representation, e.g., "rwxr-xr-x".
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    String permissions;

    /**
     * Date/time when entity was created.
     */
    Instant created;

    /**
     * Date/time when entity was last modified.
     */
    Instant lastModified;

    /**
     * Date/time when entity was last accessed.
     */
    Instant lastAccessed;

    /**
     * Flag: is entry considered "hidden" when listing/walking directory contents?
     */
    boolean hidden;

    /**
     * In which Neo4Jfs file system is this entry located?
     */
    @Transient
    URI fsUri;

    /**
     * Take a set of permissions and convert to its string representation.
     * @param posixPermissions Set of Posix file permissions converted into a string representation.
     */
    public void setPosixPermissions(final Set<PosixFilePermission> posixPermissions) {
        permissions = PosixFilePermissionConverter.convert(posixPermissions);
    }

    /**
     * Permissions stored with enty is converted into a set of PosixFilePermissions.
     * @return set of PosixFilePermissions
     */
    public Set<PosixFilePermission> getPosixPermissions() {
        return PosixFilePermissionConverter.convert(permissions);
    }
}
