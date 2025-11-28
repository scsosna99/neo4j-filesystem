package dev.scottsosna.sandbox.neo4jfs.database.model.neo4j;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Neo4J database access as defined by official documentation.
 * https://neo4j.com/docs/operations-manual/current/database-administration/standard-databases/listing-databases/
 */
public enum DatabaseAccessType {
    READ_ONLY("read-only"),
    READ_WRITE("read-write");

    private final String access;

    DatabaseAccessType(String access) {
        this.access = access;
    }

    public String getAccess() {
        return access;
    }

    /**
     * Return enum for access value.
     * @param neo4jValue value returned by Neo4J
     * @return associated enum or throw exception if unknown
     */
    static public DatabaseAccessType convert(String neo4jValue) {
        var toReturn = neo4jValueMap.get(neo4jValue);
        if (toReturn == null) {
            throw new IllegalArgumentException("Unknown status: " + neo4jValue);
        } else {
            return toReturn;
        }
    }

    //  Map of status values to enum values used to convert what Neo4J into actual enum
    static final Map<String,DatabaseAccessType> neo4jValueMap;
    static {
        neo4jValueMap = Arrays.stream(values())
            .collect(Collectors.toMap(DatabaseAccessType::getAccess, e -> e));
    }
}
