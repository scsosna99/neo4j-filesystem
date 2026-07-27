/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.filesystem.attribute;

import javax.security.auth.Subject;
import java.nio.file.attribute.UserPrincipal;

/**
 * Simple UserPrincipal implementation.
 * @param name user name
 */
public record UserPrincipalImpl (String name) implements UserPrincipal {
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean implies(Subject subject) {
        return UserPrincipal.super.implies(subject);
    }
}