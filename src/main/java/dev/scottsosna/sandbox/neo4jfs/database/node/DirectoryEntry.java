package dev.scottsosna.sandbox.neo4jfs.database.node;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity( label = "Directory")
@Getter @Setter @NoArgsConstructor
public class DirectoryEntry extends BaseEntry {

    boolean root;

    @Relationship(type = "PARENT_OF")
    List<DirectoryEntry> subdirs;

    @Relationship(type = "CONTAINS")
    List<FileEntry> files;
}
