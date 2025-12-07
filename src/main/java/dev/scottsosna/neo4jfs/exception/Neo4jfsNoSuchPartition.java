package dev.scottsosna.neo4jfs.exception;

/**
 * Expected/requested Storage Manager partition does not exist.
 */
public class Neo4jfsNoSuchPartition extends Neo4jfsException {

    /**
     * Constructor
     * @param partitionName requested partition name
     */
    public Neo4jfsNoSuchPartition(String partitionName) {
        super(partitionName);
    }
}
