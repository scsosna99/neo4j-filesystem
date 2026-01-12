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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Very simple Basic Auth filter for demo purposes. Accepts any password.
 * Groups are sourced from application properties via {@link UserGroupStore}.
 */
public class BasicAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";

    private final UserGroupStore userStore;

    /**
     * Constructor
     * @param userStore maps users to groups for security context
     */
    public BasicAuthFilter(final UserGroupStore userStore) {
        this.userStore = userStore;
    }

    /**
     * Spring filtering for security
     * @param request servlet request
     * @param response servlet response
     * @param filterChain filter chain to continue processing
     * @throws ServletException thrown when servlet problem occurs
     * @throws IOException thrown when I/O error of some sort occurred
     */
    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
        throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String header = request.getHeader(AUTHORIZATION);
            if (StringUtils.hasText(header) && header.startsWith(BASIC_PREFIX)) {
                String base64Token = header.substring(BASIC_PREFIX.length());
                String token;
                try {
                    byte[] decoded = Base64.getDecoder().decode(base64Token.getBytes(StandardCharsets.UTF_8));
                    token = new String(decoded, StandardCharsets.UTF_8);
                    int delim = token.indexOf(":");
                    String username = delim != -1 ? token.substring(0, delim) : token;
                    // password is ignored intentionally for demo

                    List<String> groups = Optional.ofNullable(userStore.getUsers().get(username))
                        .orElse(Collections.singletonList("nobody"));
                    List<SimpleGrantedAuthority> authorities = groups.stream()
                        .filter(Objects::nonNull)
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .map(SimpleGrantedAuthority::new)
                        .distinct()
                        .toList();

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, "N/A", authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (IllegalArgumentException e) {
                    // Malformed header means call remains unauthenticated
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
