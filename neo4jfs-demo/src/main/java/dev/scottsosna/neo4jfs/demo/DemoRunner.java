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

import dev.scottsosna.neo4jfs.security.AccessManager;
import dev.scottsosna.neo4jfs.util.SpringContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Demo01: Create a new Neo4Jfs file system, create directories, load files.  Nothing too fancy.
 */
@SpringBootApplication(scanBasePackages = {"dev.scottsosna.neo4jfs","dev.scottsosna.neo4jfs.demo"})
public class DemoRunner implements CommandLineRunner {

    private final AccessManager accessManager;

    public DemoRunner(final AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    @Override
    public void run(String... args) {

        //  Set security context for demo to run,
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            new TestingAuthenticationToken(
                accessManager.getAdminUser(),
                "demoRunner",
                List.of(new SimpleGrantedAuthority(accessManager.getAdminGroup())))
        );
        SecurityContextHolder.setContext(context);

        //  Run demos specified on command line.
        for (String beanName: args) {
            //  Get the bean and run.
            Demo toRun = SpringContext.getBean(beanName, Demo.class);
            if (toRun != null) {
                toRun.demo();
            } else {
                System.err.println("No demo found for " + beanName);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) return;
        SpringApplication.run(DemoRunner.class, args);
    }
}
