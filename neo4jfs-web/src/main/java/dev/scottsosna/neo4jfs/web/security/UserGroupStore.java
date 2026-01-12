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
package dev.scottsosna.neo4jfs.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds demo user -> groups mapping from application properties.
 */
@Component
@ConfigurationProperties(prefix = "neo4jfs.web")
public class UserGroupStore {

    /**
     * Map of users to assigned groups as loaded from application properties.
     */
    private Map<String, List<String>> users = new HashMap<>();

    /**
     * getter
     * @return
     */
    public Map<String, List<String>> getUsers() {
        return Map.copyOf(users);
    }

    /**
     * setter
     * @param users assigns the map of users/groups used for security in web app.
     */
    public void setUsers(final Map<String, List<String>> users) {
        this.users = Map.copyOf(users);
    }
}
