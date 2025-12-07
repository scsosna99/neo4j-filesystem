package dev.scottsosna.neo4jfs.database.repository.util;

import dev.scottsosna.neo4jfs.util.Consumer5;

import java.util.Map;

/**
 * Defines multiple methods which assist in dynamically building Cypher clauses, passed as a parameter to support methods.
 */
@FunctionalInterface
public interface AddCypherClauseConsumer extends Consumer5<StringBuilder,StringBuilder, Map<String,Object>, String, Integer> {
}
