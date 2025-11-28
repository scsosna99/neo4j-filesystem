package dev.scottsosna.sandbox.neo4jfs.database.model.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter @Setter @NoArgsConstructor
public class Database {

    /**
     * Constructor that deserializes the results returned from a "SHOW DATABASE" query.  As this is
     * not a true Cypher query returning nodes/relationships, we manually deserialize.
     * @param results
     */
    public Database(Map<String,Object> results) {
        this.access = DatabaseAccessType.convert(results.get(DatabaseColumnNames.ACCESS).toString());
        this.address = results.get(DatabaseColumnNames.ADDRESS).toString();
        this.role = results.get(DatabaseColumnNames.ROLE).toString();
        this.currentStatus = DatabaseStatusType.convert(results.get(DatabaseColumnNames.CURRENT_STATUS).toString());
        this.type = DatabaseType.convert(results.get(DatabaseColumnNames.TYPE).toString());
        this.statusMessage = results.get(DatabaseColumnNames.STATUS_MESSAGE).toString();
        this.requestedStatus = DatabaseStatusType.convert(results.get(DatabaseColumnNames.REQUESTED_STATUS).toString());
        this.home = Boolean.parseBoolean(results.get(DatabaseColumnNames.HOME).toString());
        this.defaultDatabase = Boolean.parseBoolean(results.get(DatabaseColumnNames.DEFAULT).toString());
        this.name = results.get(DatabaseColumnNames.NAME).toString();
        this.writer = Boolean.parseBoolean(results.get(DatabaseColumnNames.WRITER).toString());
    }

    private DatabaseAccessType access;
    private String address;
    private String role;
    private DatabaseStatusType currentStatus;
    private DatabaseType type;
    private String statusMessage;
    private DatabaseStatusType requestedStatus;
    private Boolean home;
    //  NOTE: it's "default" in Neo4J which is reserved in Java
    private Boolean defaultDatabase;
    private String name;
    private Boolean writer;
}
