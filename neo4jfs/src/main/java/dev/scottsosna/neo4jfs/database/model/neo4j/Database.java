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
package dev.scottsosna.neo4jfs.database.model.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Neo4J Database model holding the values returned by "SHOW DATABASE" command.
 */
@Getter @NoArgsConstructor
public class Database {

    /**
     * Database is either read-write or read-only.
     */
    private DatabaseAccessType access;

    /**
     * Host name and port where database can be found.
     */
    private String address;

    /**
     * Database role - primary, secondary, unknown - usually primary
     */
    private String role;

    /**
     * Enum defines the valid statuses, only online is useful.
     */
    private DatabaseStatusType currentStatus;

    /**
     * Type of database: system, standard, composite
     */
    private DatabaseType type;

    /**
     * When available, explains why database is not in correct state.
     */
    private String statusMessage;

    /**
     * Enum defines status that may be requested for a database.
     */
    private DatabaseStatusType requestedStatus;

    /**
     * When true, home database for the current users
     */
    private Boolean home;

    /**
     * When true, this database is the default database for the current user.
     * Attribute is different name than in Neo4J because "default" is a reserved word in Java.
     */
    private Boolean defaultDatabase;

    /**
     * database name
     */
    private String name;

    /**
     * true for standalone instance or for instance in cluster accepting writes for this database
     */
    private Boolean writer;

    /**
     * Constructor that deserializes the results returned from a "SHOW DATABASE" query.  As this is
     * not a true Cypher query returning nodes/relationships, we manually deserialize.
     * @param results the raw data returned from Neo4J.
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
}
