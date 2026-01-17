# Licensing Requirements
This software is available under the [MIT license](https://opensource.org/license/MIT) for **non-commercial** use only.

This software is available under the [GPLv3 license](https://www.gnu.org/licenses/gpl-3.0.html) for all **commercial** use *unless* a separate license is acquired from its owner, [Scott C. Sosna](license@scottsosna.dev).

# Overview 
`Neo4Jfs` is a fully-functioning Java file system based on `java.nio.file.FileSystem` architecture.  The file tree is managed as nodes and vectors persisted to a Neo4J database, while the file contents are stored separately in an external store (i.e., [AWS S3](https://aws.amazon.com/s3/), [Azure Blob Storage](https://azure.microsoft.com/en-us/products/storage/blobs), local disk, etc.).

As a Java file system, JVM-based solutions use `java.nio.file.File` functionality to manage files through well-known Java APIs: create directories, upload/download files, move files/directories, etc.  `java.nio.file.Files` bridges file systems, for example copying files from a local disk into `Neo4Jfs` simply by calling `java.nio.file.File.copy()` without requiring your application to open/read/write the local file:

`java.nio.file.Files.copy(Path.of("/local/file/path"), Path.of("neo4jfs://partition/path/to/file"))`

# Motivation
At multiple jobs, I've worked on SaaS solutions that allows customers to manager documents in a virtual file system: create directories, upload/download files, apply security, etc.  The file contents are stored externally, usually in cloud-based storage or some other form of blob storage.  The virtual file system is implemented as a customized one-off with the minimum functionality required.

These custom implementations have numerous issues:
* The file tree is persisted in a database in a form that introduces various performance problems, such as navigating a deep tree or moving files and directories (especially a large number of files).
* Non-standard, customized APIs for managing file system require additional development effort.
* Customized implementations are more difficult to maintain and extend.

`Neo4Jfs` attempts to resolve these issues by providing a standardized, fully-functional Java file system backed by a graph database which can quickly and easily navigate an arbitrarily deep directory structure.  `Noe4Jfs` stores directory and file entries as *nodes* with relationships to make a simple graph.  Pathnames and permissions are derived at query time rather than stored explicitly with each entry, allowing a directory move to be nothing more than changing a relationship.

# Getting Started

## Technical Requirements
* Java 21
* Neo4J Database instance

*Neo4J Database Note*: `Neo4Jfs` automatically creates separate databases for each file system partition programmatically.  Unfortunately, Neo4J restricts this capability to [Neo4J Enterprise Edition](https://neo4j.com/pricing/) or [Neo4J Desktop](https://neo4j.com/docs/desktop/current/).  The community version and Neo4J AuraDB Free so not allow databases to be created programatically.

You *can* pre-create the databases manually if necessary, where the database name and the `Neo4Jfs` partition name are the same. 

## Project Structure

The `Neo4Jfs` project is divided into three modules/sub-projects:
* `neo4jfs`: the core file system implementation.
* `neo4jfs-demo`: simple examples of using `Neo4Jfs` using `java.nio.file.Files`.
* `neo4jfs-web`: a bare-bones web app exposing `Neo4JFfs` via REST APIs.  The app exposes minimum functionality.  The app cannot be used in production without significant security enhancements and more appropriate error handling.  **You have been warned!**

## URI Details
The URI scheme for `Noe4Jfs` is `neo4jfs`.  A complete URI has the form `neo4jfs://[partition]/directory/path` where `[partition]` segregates different virtual file systems by dedicating a `Neo4J` database per partition.  `Neo4Jfs` will automatically create the database if it does not already exist.

## Storage Manager

The `Noe4Jfs` storage manager is responsible for writing/reading file contents to/from external storage.  The project supports two different storage manager:
* **local**: the default storage manager using local disk storage, location specified by the location specified by `neo4jfs.local.directory` or from current working directory. 
* **dummy**: the no-op storage manager that does not persist file contents to an external location, useful when testing file tree functionality and actual file contents are not important.

A custom storage manager can be implemented by implementing the `dev.scottsosna.neo4js.storage.StorageManager` interface.

## Usage

The basic pattern is to load a `Neo4Jfs` file system and making calls to `java.nio.file.Files` to do whatever file operations desired.

`try (FileSystem fs = FileSystems.newFileSystem(URI.create("neo4jfs://neo4jfs-demo"), Map.of())) {
    Files.createDirectory(fs.getPath("/songs"));
 }
`

The above code loads a `Neo4Jfs` file system for the partition (database) `neo4jfs-demo` which loads the file system within the currently running JVM.  After the `Neo4Jfs` is ready, an attempt to create the `songs` directory is made.

Refer to [application.xml](neo4jfs/src/main/resources/application.properties) for `Neo4Jfs` configuration information.

See the [Neo4Jfs Demo](neo4jfs-demo/README.md) for more examples of using `Neo4Jfs` with `java.nio.file.Files`

# Future Work
* Add support for soft links and other special file types (e.g., URL bookmarks seems like a good candidate)
* Extend security beyond simple Posix permissions to support ACLS.
* Add additional APIs to sample web app for more than the most simple file management.
* Detailed security testing.  If there's any area that may have issues, it's security. 
* Caching Cypher statements generated, avoid re-generating them for every query.
* More than just perfunctory performance testing.  Benchmark against other file systems, load testing, access testing, etc.
* Unit tests.
