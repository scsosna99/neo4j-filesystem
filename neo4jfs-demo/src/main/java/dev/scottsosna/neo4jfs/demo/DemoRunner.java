package dev.scottsosna.neo4jfs.demo;

import dev.scottsosna.neo4jfs.util.SpringContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
