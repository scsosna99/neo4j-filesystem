/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.demo;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Base class for all individual demo instances
 */
public abstract class Demo {

    /**
     * Method implemented for each demo functionality
     */
    abstract void demo();

    /**
     * Sets/reset security context so Neo4Jfs operations can be executed under different users.
     * @param userName to apply to security context
     * @param groupName to apply to security context
     */
    protected void setSecurityContext(final String userName,
                                      final String groupName) {
        //  Set security context for demo to run,
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            new TestingAuthenticationToken(
                userName,
                "demoRunner",
                List.of(new SimpleGrantedAuthority(groupName)))
        );
        SecurityContextHolder.setContext(context);

    }
}
