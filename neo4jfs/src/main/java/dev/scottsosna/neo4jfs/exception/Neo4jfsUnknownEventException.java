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

/**
 * Neo4Jfs exception indicating an unknown/unexpected event type encountered.
 */
public class Neo4jfsUnknownEventException extends Neo4jfsException {
    public Neo4jfsUnknownEventException() {
        super();
    }

    public Neo4jfsUnknownEventException(String message) {
        super(message);
    }

    public Neo4jfsUnknownEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public Neo4jfsUnknownEventException(Throwable cause) {
        super(cause);
    }
}
