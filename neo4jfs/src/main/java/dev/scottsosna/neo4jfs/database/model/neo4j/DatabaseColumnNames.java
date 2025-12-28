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

/**
 * Column names for the data that comes back from a "SHOW DATABASE" query as defined by
 * https://neo4j.com/docs/operations-manual/current/database-administration/standard-databases/listing-databases/
 */
public class DatabaseColumnNames {

    public final static String ACCESS = "access";
    public final static String ADDRESS = "address";
    public final static String ALIASES = "aliases";
    public final static String CONSTITUENTS = "constituents";
    public final static String CURRENT_STATUS = "currentStatus";
    public final static String DEFAULT = "default"; // NOTE: Java keyword
    public final static String HOME = "home";
    public final static String NAME = "name";
    public final static String OWNER = "owner";
    public final static String REQUESTED_STATUS = "requestedStatus";
    public final static String ROLE = "role";
    public final static String STATUS_MESSAGE ="statusMessage";
    public final static String TYPE = "type";
    public final static String WRITER = "writer";

}
