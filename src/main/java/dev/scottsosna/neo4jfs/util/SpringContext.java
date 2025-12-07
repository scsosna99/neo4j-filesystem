package dev.scottsosna.neo4jfs.util;

import dev.scottsosna.neo4jfs.config.Neo4jfsConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import static dev.scottsosna.neo4jfs.config.Neo4jfsConstants.NEO4JFS_PROPERTY_PAGINATION_SIZE;

/**
 * Utility class for retrieving Spring beans by non-Spring managed objects
 */
@Component
public class SpringContext implements ApplicationContextAware {

    /**
     * Spring application context
     */
    private static ApplicationContext context;

    /**
     * Configuration instance.
     */
    private static Neo4jfsConfiguration config;

    /**
     * Constructor.
     * @param config
     */
    public SpringContext(final Neo4jfsConfiguration config) {
        SpringContext.config = config;
    }

    /**
     * Saves the context for use statically.
     * @param applicationContext the ApplicationContext object to be used by this object
     */
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        SpringContext.context = applicationContext;
    }

    /**
     * Returns a Spring-instantiated bean for the class specified.
     * @param beanClass bean class to retrieve from context
     * @return Spring bean or null if not found
     */
    public static <T extends Object> T getBean(final Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    /**
     * Returns a specific configuration property.
     * @param propertyName property name
     * @return currently-configured value.
     */
    public static Integer getPropertyInteger(final String propertyName) {
        switch (propertyName) {
            case NEO4JFS_PROPERTY_PAGINATION_SIZE:
                return config.defaultPageSize;
            default:
                throw new IllegalArgumentException("Unknown configuration property: %s".formatted(propertyName));
        }
    }
}
