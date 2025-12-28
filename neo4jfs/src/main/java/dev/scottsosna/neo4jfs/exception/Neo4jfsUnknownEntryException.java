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
package dev.scottsosna.neo4jfs.exception;

import java.net.URI;

/**
 * Neo4Jfs exception signalling unknown entry type.
 */
public class Neo4jfsUnknownEntryException extends Neo4jfsException {
    public Neo4jfsUnknownEntryException() {
        super();
    }

    /**
     * Constructor for composing exception message.
     * @param uri Neo4Jfs URI for entry
     * @param entryType unknown/unexpected/unsupported entry type
     */
    public Neo4jfsUnknownEntryException(URI uri, String entryType) {
        super(String.format("Unknown entry type '%s' for URI '%s'", entryType, uri));
    }

    public Neo4jfsUnknownEntryException(String message) {
        super(message);
    }

    public Neo4jfsUnknownEntryException(String message, Throwable cause) {
        super(message, cause);
    }

    public Neo4jfsUnknownEntryException(Throwable cause) {
        super(cause);
    }
}
