package dev.scottsosna.neo4jfs.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility class for retrieving Spring beans, usually outside of Spring-managed components.
 */
@Component
public class SpringContext implements ApplicationContextAware {

    private static ApplicationContext context;

    /**
     * Returns a Spring-instantiated bean for the class specified.
     * @param beanClass bean class to retrieve from context
     * @return Spring bean or null if not found
     */
    public static <T extends Object> T getBean(final Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    /**
     * Saves the context for use statically.
     * @param applicationContext the ApplicationContext object to be used by this object
     */
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        SpringContext.context = applicationContext;
    }
}
