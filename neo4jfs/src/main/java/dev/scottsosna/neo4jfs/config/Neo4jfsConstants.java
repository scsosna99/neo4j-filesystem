package dev.scottsosna.neo4jfs.config;

import java.util.List;

/**
 * Neo4jfs constants stored in a centralized, standard location.
 */
public class Neo4jfsConstants {

    /**
     * The defacto name of the root directory.
     */
    public static final String NAME_ROOT_DIRECTORY = "/";

    /**
     * Using standard *nix admin group "wheel" for all uber-group permissioning.
     */
    public static final String NAME_ADMIN_GROUP = "wheel";

    /**
     * Using standard *nix admin user "root" for all uber-user permissioning.
     */
    public static final String NAME_ADMIN_USER = "root";

    /**
     * An unknown or unauthenticated group.
     */
    public static final String NAME_UNAUTHENTICATED_GROUP = "nobody";

    /**
     * An unknown or unauthenticated user.
     */
    public static final String NAME_UNAUTHENTICATED_USER = "nobody";

    /**
     * What character separates path elements in Neo4Jfs.
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * The URI scheme required by Neo4J file system.  URIs through must provide this.
     */
    public static final String NEO4JFS_URI_SCHEME = "neo4jfs";

    /**
     * Parameters used in Cypher queries.
     */
    public static final String CYPHER_PARAM_NODEID = "id";
    public static final String CYPHER_PARAM_NODEID_END = "endId";
    public static final String CYPHER_PARAM_NODEID_START = "startId";
    public static final String CYPHER_PARAM_NAME = "name";
    public static final String CYPHER_PARAM_PAGINATION_LIMIT = "limit";
    public static final String CYPHER_PARAM_PAGINATION_SKIP = "skip";

    /**
     * Spring-configured properties that can be retrieved ny Spring context.
     */
    public static final String NEO4JFS_PROPERTY_PAGINATION_SIZE = "neo4jfs.pagination.size";

    /**
     * Magical characters for specifying attributes in a string as defined by {@code java.nio.file.Files.readAttributes}
     */
    public static final String ATTRIBUTE_SEPARATOR = ",";
    public static final String ATTRIBUTE_VIEW_SEPARATOR = ":";
    public static final String ATTRIBUTE_WILDCARD_ALL = "*";

    /**
     * Supported attribute view names
     */
    public static final String ATTRIBUTE_VIEW_NAME_BASIC = "basic";

    /**
     * The valid view names when attribute list specifies a view name for the attributes.
     */
    public static final List<String> SUPPORTED_ATTRIBUTE_VIEW_NAME = List.of(ATTRIBUTE_VIEW_NAME_BASIC);

    /**
     * What to default to when view name is not specified.
     */
    public static final String DEFAULT_ATTRIBUTE_VIEW_NAME = "basic";

    /**
     * The string equivalents for attributes exposed by {@code BasicFileAttributes}
     */
    public static final String BASIC_ATTRIBUTE_CREATE_TIME = "creationTime";
    public static final String BASIC_ATTRIBUTE_FILE_KEY = "fileKey";
    public static final String BASIC_ATTRIBUTE_LAST_ACCESS_TIME = "lastAccessTime";
    public static final String BASIC_ATTRIBUTE_LAST_MODIFIED_TIME = "lastModifiedTime";
    public static final String BASIC_ATTRIBUTE_IS_DIRECTORY = "isDirectory";
    public static final String BASIC_ATTRIBUTE_IS_OTHER = "isOther";
    public static final String BASIC_ATTRIBUTE_IS_REGULAR_FILE = "isRegularFile";
    public static final String BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK = "isSymbolicLink";
    public static final String BASIC_ATTRIBUTE_SIZE = "size";
    public static final List<String> BASIC_ATTRIBUTES_ALL = List.of(
        BASIC_ATTRIBUTE_CREATE_TIME,
        BASIC_ATTRIBUTE_FILE_KEY,
        BASIC_ATTRIBUTE_LAST_ACCESS_TIME,
        BASIC_ATTRIBUTE_LAST_MODIFIED_TIME,
        BASIC_ATTRIBUTE_IS_DIRECTORY,
        BASIC_ATTRIBUTE_IS_OTHER,
        BASIC_ATTRIBUTE_IS_REGULAR_FILE,
        BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK,
        BASIC_ATTRIBUTE_SIZE
    );

    /**
     * Using Posix permission strings to represent Neo4Jfs permissions.
     */
    public static final char NEO4JFS_PERMISSION_READ = 'r';
    public static final char NEO4JFS_PERMISSION_WRITE = 'w';
    public static final char NEO4JFS_PERMISSION_EXECUTE = 'x';
    public static final char NEO4JFS_PERMISSION_NONE = '-';
    public static final String NEO4JFS_PERMISSION_NONE_GROUP = "---";
}
