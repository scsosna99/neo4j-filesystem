/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.filesystem.attribute;

import dev.scottsosna.neo4jfs.database.node.BaseEntry;

import java.io.IOException;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;

/**
 * Neo4Jfs implementation of FileOwnerAttributeView.
 */
public class FileOwnerAttributeViewImpl extends BasicFileAttributeViewImpl implements FileOwnerAttributeView {

    public static final String VIEW_NAME = "owner";

    /**
     * Constructor
     * @param entry file/directory providing the attributes.
     */
    public FileOwnerAttributeViewImpl(BaseEntry entry) {
        super(entry);
    }

    /**
     * Returns the name of the attribute view. Attribute views of this type have the name "owner".
     * @return name of the attribute view
     */
    @Override
    public String name() {
        return VIEW_NAME;
    }

    /**
     * @return the owner for this file/directory
     * @throws IOException
     */
    @Override
    public UserPrincipal getOwner() throws IOException {
        return new UserPrincipalImpl(entry.getOwnerUserName());
    }

    /**
     * Update file/directory owner.
     * @param owner the new file/directory owner
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void setOwner(UserPrincipal owner) throws IOException {
        entry.setOwnerUserName(owner.getName());
        persist();
    }
}
