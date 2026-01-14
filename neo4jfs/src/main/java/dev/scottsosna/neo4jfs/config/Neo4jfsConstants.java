/*
 * Copyright 2025 Scott C. Sosna.  All rights reserved.
 *
 * Licensed under the MIT license for non-commercial use.  Please refer to LICENSE-MIT.md or
 * https://opensource.org/license/mit for terms and conditions.
 *
 * Licensed under the GPLv3 license for commercial use.  Please refer to LICENSE-GPL.md or
 * https://www.gnu.org/licenses/gpl-3.0.html for terms and conditions.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * expressed or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.scottsosna.neo4jfs.config;

import dev.scottsosna.neo4jfs.filesystem.attribute.BasicFileAttributeViewImpl;
import dev.scottsosna.neo4jfs.filesystem.attribute.FileOwnerAttributeViewImpl;
import dev.scottsosna.neo4jfs.filesystem.attribute.PosixFileAttributeViewImpl;

import java.util.List;
import java.util.Set;

/**
 * Neo4jfs constants stored in a centralized, standard location.
 */
public class Neo4jfsConstants {

    /**
     * The defacto name of the root directory.
     */
    public static final String NAME_ROOT_DIRECTORY = "/";

    /**
     * An unknown or unauthenticated group.
     */
    public static final String NAME_UNAUTHENTICATED_GROUP = "nobody";

    /**
     * An unknown or unauthenticated user.
     */
    public static final String NAME_UNAUTHENTICATED_USER = "nobody";

    /**
     * What character separates path elements in Neo4Jfs.
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * The URI scheme required by Neo4J file system.  URIs through must provide this.
     */
    public static final String NEO4JFS_URI_SCHEME = "neo4jfs";

    /**
     * Format of the Neo4Jfs URI with the partition ID to be substituted in.
     */
    public static final String NEO4J_URI_TEMPLATE = "neo4jfs://%s/";



    /**
     * Parameters used in Cypher queries.
     */
    public static final String CYPHER_PARAM_NODEID = "id";
    public static final String CYPHER_PARAM_NODEID_END = "endId";
    public static final String CYPHER_PARAM_NODEID_START = "startId";
    public static final String CYPHER_PARAM_NAME = "name";
    public static final String CYPHER_PARAM_PAGINATION_LIMIT = "limit";
    public static final String CYPHER_PARAM_PAGINATION_SKIP = "skip";

    /**
     * Spring-configured properties that can be retrieved ny Spring context.
     */
    public static final String NEO4JFS_PROPERTY_PAGINATION_SIZE = "neo4jfs.pagination.size";

    /**
     * Magical characters for specifying attributes in a string as defined by {@code java.nio.file.Files.readAttributes}
     */
    public static final String ATTRIBUTE_SEPARATOR = ",";
    public static final String ATTRIBUTE_VIEW_SEPARATOR = ":";
    public static final String ATTRIBUTE_WILDCARD_ALL = "*";

    /**
     * The valid view names when attribute list specifies a view name for the attributes.
     */
    public static final Set<String> SUPPORTED_ATTRIBUTE_VIEW_NAMES = Set.of(
        BasicFileAttributeViewImpl.VIEW_NAME,
        FileOwnerAttributeViewImpl.VIEW_NAME,
        PosixFileAttributeViewImpl.VIEW_NAME
    );

    /**
     * What to default to when view name is not specified.
     */
    public static final String DEFAULT_ATTRIBUTE_VIEW_NAME = BasicFileAttributeViewImpl.VIEW_NAME;

    /**
     * The string equivalents for attributes exposed by {@code BasicFileAttributes}
     */
    public static final String BASIC_ATTRIBUTE_CREATE_TIME = "creationTime";
    public static final String BASIC_ATTRIBUTE_FILE_KEY = "fileKey";
    public static final String BASIC_ATTRIBUTE_LAST_ACCESS_TIME = "lastAccessTime";
    public static final String BASIC_ATTRIBUTE_LAST_MODIFIED_TIME = "lastModifiedTime";
    public static final String BASIC_ATTRIBUTE_IS_DIRECTORY = "isDirectory";
    public static final String BASIC_ATTRIBUTE_IS_OTHER = "isOther";
    public static final String BASIC_ATTRIBUTE_IS_REGULAR_FILE = "isRegularFile";
    public static final String BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK = "isSymbolicLink";
    public static final String BASIC_ATTRIBUTE_SIZE = "size";
    public static final List<String> BASIC_ATTRIBUTES_ALL = List.of(
        BASIC_ATTRIBUTE_CREATE_TIME,
        BASIC_ATTRIBUTE_FILE_KEY,
        BASIC_ATTRIBUTE_LAST_ACCESS_TIME,
        BASIC_ATTRIBUTE_LAST_MODIFIED_TIME,
        BASIC_ATTRIBUTE_IS_DIRECTORY,
        BASIC_ATTRIBUTE_IS_OTHER,
        BASIC_ATTRIBUTE_IS_REGULAR_FILE,
        BASIC_ATTRIBUTE_IS_SYMBOLIC_LINK,
        BASIC_ATTRIBUTE_SIZE
    );

    /**
     * The string equivalents exposed by {@code FileOwnerAttributeView}
     */
    public static final String POSIX_ATTRIBUTE_GROUP = "group";
    public static final String POSIX_ATTRIBUTE_OWNER = "owner";
    public static final List<String> POSIX_ATTRIBUTES_ALL = List.of(
        POSIX_ATTRIBUTE_GROUP,
        POSIX_ATTRIBUTE_OWNER);
}
