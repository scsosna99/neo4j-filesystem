package dev.scottsosna.neo4jfs.filesystem;


import java.io.IOException;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;

/**
 * Neo4Jfs implementation of the FileOwnerAttributeView interface.
 */
public class Neo4jfsFileOwnerAttributeView implements FileOwnerAttributeView {

    /**
     * user stored as a {@code UserPrincipal}
     */
    private UserPrincipal owner;

    /**
     * Constructor.
     * @param name
     */
    public Neo4jfsFileOwnerAttributeView (final String name) {
        this.owner = new Neo4jfsUserPrincipal(name);
    }

    /**
     * @return name of the view.
     */
    @Override
    public String name() {
        return "FileOwnerAttributeView";
    }

    /**
     * getter
     * @return owner of this file or directory.
     * @throws IOException
     */
    @Override
    public UserPrincipal getOwner() throws IOException {
        return owner;
    }

    /**
     * Setter,  Theoretically this updates the owner in the database, but would need to gather more context to do so.
     * @param owner the new file owner
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
        this.owner = owner;
    }

    /**
     * Neo4Jfs implementation of the UserPrincipal interface.
     */
    private class Neo4jfsUserPrincipal implements UserPrincipal {

        /**
         * User name of the file/directory owner.
         */
        private final String name;

        /**
         * Constructor.
         * @param name User name of the file/directory owner.
         */
        Neo4jfsUserPrincipal(String name) {
            this.name = name;
        }

        /**
         * Getter
         * @return owner's user name
         */
        @Override
        public String getName() {
            return name;
        }
    }
}
