package dev.scottsosna.neo4jfs.database.model.neo4j;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Neo4J database status as defined by official documentation.
 * https://neo4j.com/docs/operations-manual/current/database-administration/standard-databases/listing-databases/
 */
public enum DatabaseStatusType {
    ONLINE("online"),
    OFFLINE("offline"),
    STARTOMG("starting"),
    STOPPING("stopping"),
    STORE_COPYING("store copying"),
    INITIAL("initial"),
    DEALLOCATING("deallocating"),
    DIRTY("dirty"),
    QUARANTINED("quarantined"),
    UNKNOWN("unknown");

    private final String statusValue;
    DatabaseStatusType(String statusValue) {
        this.statusValue = statusValue;
    }

    public String getStatusValue() {
        return statusValue;
    }

    /**
     * Return enum for status value.
     * @param neo4jValue value returned by Neo4J
     * @return associated enum or throw exception if unknown
     */
    static public DatabaseStatusType convert(String neo4jValue) {
        var toReturn = neo4jValueMap.get(neo4jValue);
        if (toReturn == null) {
            throw new IllegalArgumentException("Unknown status: " + neo4jValue);
        } else {
            return toReturn;
        }
    }

    //  Map of status values to enum values used to convert what Neo4J into actual enum
    static final Map<String,DatabaseStatusType> neo4jValueMap;
    static {
        neo4jValueMap = Arrays.stream(values())
            .collect(Collectors.toMap(DatabaseStatusType::getStatusValue, e -> e));
    }
}
