package dev.scottsosna.neo4jfs.database.node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.id.UuidStrategy;

import java.time.Instant;

@NodeEntity( label = "Base")
@Getter @Setter @NoArgsConstructor
public class BaseEntry {

    @Id
    @GeneratedValue(strategy = UuidStrategy.class)
    String id;
    String name;
    String userName;
    String groupName;
    Instant created;
    Instant lastModified;
    Instant lastAccessed;
}
