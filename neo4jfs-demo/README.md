The neo4jfs-demo module contains mini programs demonstrating basic functionality of Neo4Jfs and how it might be incorporated into a larger application.

The `demo-runner` program takes one or more demos as arguments and runs each of them in sequence.
* Demo01: Create a new Neo4Jfs file system, create directories, load files.  Nothing too fancy.
* Demo02: Create a new Neo4Jfs file system, create directories, load files.  Again, nothing too fancy.
* Demo03: Posix permissions demo where permissions are inherited where they aren't defined on directory/file/.
* Demo04: Temp directories and files.
* Demo05: Show Neo4Jfs security in action by creating different users, uploading files, and demonstrating permissions allowing/disallowing operations.  Simple but instructive.
* Demo06: copies user's home into Neo4Jfs, useful for perf testing Neo4Jfs side of things.