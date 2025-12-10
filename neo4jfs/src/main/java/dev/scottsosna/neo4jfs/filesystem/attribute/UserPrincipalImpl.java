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