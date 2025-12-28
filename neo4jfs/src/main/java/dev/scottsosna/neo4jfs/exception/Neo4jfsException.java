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

import java.io.IOException;

/**
 * Base Neo4Jfs exception to handle when Java NIO doesn't have something appropriate.
 */
public class Neo4jfsException extends IOException {
    public Neo4jfsException() {
        super();
    }

    public Neo4jfsException(String message) {
        super(message);
    }

    public Neo4jfsException(String message, Throwable cause) {
        super(message, cause);
    }

    public Neo4jfsException(Throwable cause) {
        super(cause);
    }
}
