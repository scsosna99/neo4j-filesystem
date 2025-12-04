package dev.scottsosna.neo4jfs.database.node;

public class FileBuilder {

    private String name;
    private String storageId;
    private Long size;

    public FileEntry build() {
        FileEntry file = new FileEntry();
        file.name = name;
        file.storageId = storageId;
        file.size = size;
        file.hidden = false;
        return file;
    }

    public FileBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FileBuilder setStorageId(String storageId) {
        this.storageId = storageId;
        return this;
    }

    public FileBuilder setSize(Long size) {
        this.size = size;
        return this;
    }
}
