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
package dev.scottsosna.neo4jfs.demo;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Base class for all individual demo instances
 */
public abstract class Demo {

    /**
     * Method implemented for each demo functionality
     */
    abstract void demo();

    /**
     * Sets/reset security context so Neo4Jfs operations can be executed under different users.
     * @param userName to apply to security context
     * @param groupName to apply to security context
     */
    protected void setSecurityContext(final String userName,
                                      final String groupName) {
        //  Set security context for demo to run,
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            new TestingAuthenticationToken(
                userName,
                "demoRunner",
                List.of(new SimpleGrantedAuthority(groupName)))
        );
        SecurityContextHolder.setContext(context);

    }
}
