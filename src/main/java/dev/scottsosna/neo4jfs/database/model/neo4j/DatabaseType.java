package dev.scottsosna.neo4jfs.database.model.neo4j;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Neo4J database type as defined by official documentation.
 * https://neo4j.com/docs/operations-manual/current/database-administration/standard-databases/listing-databases/
 */
public enum DatabaseType {
    SYSTEM("system"),
    STANDARD("standard"),
    COMPOSITE("composite");

    /**
     * Database type value as returned by Neo4J.
     */
    private final String type;

    /**
     * Maps Neo4J type values to enum, used when converting what was returned by Neo4J.
     */
    static final Map<String,DatabaseType> neo4jValueMap;

    /**
     * Constructor
     * @param type Neo4J type value for the enum.
     */
    DatabaseType(String type) {
        this.type = type;
    }

    /**
     * Getter
     * @return Neo4J type value for this enum.
     */
    public String getType() {
        return type;
    }

    /**
     * Return enum for type value.
     * @param neo4jValue value returned by Neo4J
     * @return associated enum or throw exception if unknown
     */
    static public DatabaseType convert(String neo4jValue) {
        var toReturn = neo4jValueMap.get(neo4jValue);
        if (toReturn == null) {
            throw new IllegalArgumentException("Unknown type: " + neo4jValue);
        } else {
            return toReturn;
        }
    }

    /**
     * Loads static map of type values to enum values.
     */
    static {
        neo4jValueMap = Arrays.stream(values()).
            collect(Collectors.toMap(DatabaseType::getType, e -> e));
    }
}
