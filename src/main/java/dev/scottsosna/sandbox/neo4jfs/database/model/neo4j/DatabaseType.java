package dev.scottsosna.sandbox.neo4jfs.database.model.neo4j;

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

    private final String type;
    DatabaseType(String type) {
        this.type = type;
    }

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

    //  Map of status values to enum values used to convert what Neo4J into actual enum
    static final Map<String,DatabaseType> neo4jValueMap;
    static {
        neo4jValueMap = Arrays.stream(values()).
            collect(Collectors.toMap(DatabaseType::getType, e -> e));
    }

}
