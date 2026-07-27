/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Neo4Jfs licensed under the MIT license.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 */
package dev.scottsosna.neo4jfs.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds demo user -> groups mapping from application properties.
 */
@Component
@ConfigurationProperties(prefix = "neo4jfs.web")
public class UserGroupStore {

    /**
     * Map of users to assigned groups as loaded from application properties.
     */
    private Map<String, List<String>> users = new HashMap<>();

    /**
     * getter
     * @return
     */
    public Map<String, List<String>> getUsers() {
        return Map.copyOf(users);
    }

    /**
     * setter
     * @param users assigns the map of users/groups used for security in web app.
     */
    public void setUsers(final Map<String, List<String>> users) {
        this.users = Map.copyOf(users);
    }
}
