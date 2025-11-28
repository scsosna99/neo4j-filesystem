package dev.scottsosna.sandbox.neo4jfs.config;

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
     * The URI scheme required by Neo4J file system.  URIs through must provide this.
     */
    public static final String NEO4JFS_URI_SCHEME = "neo4jfs";
}
