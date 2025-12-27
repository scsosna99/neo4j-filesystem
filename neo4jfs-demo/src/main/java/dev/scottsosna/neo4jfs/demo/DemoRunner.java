package dev.scottsosna.neo4jfs.demo;

import dev.scottsosna.neo4jfs.config.Neo4jfsConstants;
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

    public static void main(String[] args) {
        if (args.length == 0) return;
        SpringApplication.run(DemoRunner.class, args);
    }

    @Override
    public void run(String... args) {

        //  Set security context for demo to run,
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            new TestingAuthenticationToken(
                Neo4jfsConstants.NAME_ADMIN_USER,
                "demoRunner",
                List.of(new SimpleGrantedAuthority(Neo4jfsConstants.NAME_ADMIN_GROUP)))
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
}
