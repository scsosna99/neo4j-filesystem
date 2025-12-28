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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Neo4J database status as defined by official documentation.
 * https://neo4j.com/docs/operations-manual/current/database-administration/standard-databases/listing-databases/
 */
public enum DatabaseStatusType {
    ONLINE("online"),
    OFFLINE("offline"),
    STARTOMG("starting"),
    STOPPING("stopping"),
    STORE_COPYING("store copying"),
    INITIAL("initial"),
    DEALLOCATING("deallocating"),
    DIRTY("dirty"),
    QUARANTINED("quarantined"),
    UNKNOWN("unknown");

    /**
     * Value returned by Neo4J
     */
    private final String statusValue;

    //  Map of status values to enum values used to convert what Neo4J into actual enum
    /**
     * Maps Neo4J status values to enum, used when converted what was returned by Neo4J.
     */
    static final Map<String,DatabaseStatusType> neo4jValueMap;

    /**
     * Constructor
     * @param statusValue Neo4J status value for this enum
     */
    DatabaseStatusType(final String statusValue) {
        this.statusValue = statusValue;
    }

    /**
     * Getter
     * @return Neop4J status value
     */
    public String getStatusValue() {
        return statusValue;
    }

    /**
     * Return enum for status value.
     * @param neo4jValue value returned by Neo4J
     * @return associated enum or throw exception if unknown
     */
    static public DatabaseStatusType convert(final String neo4jValue) {
        var toReturn = neo4jValueMap.get(neo4jValue);
        if (toReturn == null) {
            throw new IllegalArgumentException("Unknown status: " + neo4jValue);
        } else {
            return toReturn;
        }
    }

    /**
     * Loads static map of status values to enum values.
     */
    static {
        neo4jValueMap = Arrays.stream(values())
            .collect(Collectors.toMap(DatabaseStatusType::getStatusValue, e -> e));
    }
}
