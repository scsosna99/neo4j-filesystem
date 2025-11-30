package dev.scottsosna.neo4jfs.database.node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity( label = "File")
@Getter @Setter @NoArgsConstructor
public class FileEntry extends BaseEntry {

    String storageId;
    long size;
}
