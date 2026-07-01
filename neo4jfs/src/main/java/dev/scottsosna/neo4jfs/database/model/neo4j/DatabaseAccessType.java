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
 * Neo4J database access as defined by official documentation.
 * <a href="https://neo4j.com/docs/operations-manual/current/database-administration/standard-databases/listing-databases/">...</a>
 */
public enum DatabaseAccessType {
    READ_ONLY("read-only"),
    READ_WRITE("read-write");

    /**
     * Access value returned by Neo4J.
     */
    private final String accessValue;

    /**
     * Maps access values to enum values, used to convert what Neo4J returns int enum.
     */
    static final Map<String,DatabaseAccessType> neo4jValueMap;

    /**
     * Constructor
     * @param accessValue Neo4J access value for this enum
     */
    DatabaseAccessType(final String accessValue) {
        this.accessValue = accessValue;
    }

    /**
     * Getter
     * @return Neo4J access value
     */
    public String getAccessValue() {
        return accessValue;
    }

    /**
     * Return enum for access value.
     * @param neo4jValue value returned by Neo4J
     * @return associated enum or throw exception if unknown
     */
    static public DatabaseAccessType convert(final String neo4jValue) {
        var toReturn = neo4jValueMap.get(neo4jValue);
        if (toReturn == null) {
            throw new IllegalArgumentException("Unknown status: " + neo4jValue);
        } else {
            return toReturn;
        }
    }

    /**
     * Loads static map of access values to enum values.
     */
    static {
        neo4jValueMap = Arrays.stream(values())
            .collect(Collectors.toMap(DatabaseAccessType::getAccessValue, e -> e));
    }
}
