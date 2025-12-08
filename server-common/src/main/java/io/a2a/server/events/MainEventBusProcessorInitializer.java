package io.a2a.server.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Portable CDI initializer for MainEventBusProcessor.
 * <p>
 * This bean observes the ApplicationScoped initialization event and injects
 * MainEventBusProcessor, which triggers its eager creation and starts the background thread.
 * </p>
 * <p>
 * This approach is portable across all Jakarta CDI implementations (Weld, OpenWebBeans, Quarkus, etc.)
 * and ensures MainEventBusProcessor starts automatically when the application starts.
 * </p>
 */
@ApplicationScoped
public class MainEventBusProcessorInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainEventBusProcessorInitializer.class);

    @Inject
    MainEventBusProcessor processor;

    /**
     * Observes ApplicationScoped initialization to force eager creation of MainEventBusProcessor.
     * The injection of MainEventBusProcessor in this bean triggers its creation, and calling
     * ensureStarted() forces the CDI proxy to be resolved, which ensures @PostConstruct has been
     * called and the background thread is running.
     */
    void onStart(@Observes @Initialized(ApplicationScoped.class) Object event) {
        if (processor != null) {
            // Force proxy resolution to ensure @PostConstruct has been called
            processor.ensureStarted();
            LOGGER.info("MainEventBusProcessor initialized and started");
        } else {
            LOGGER.error("MainEventBusProcessor is null - initialization failed!");
        }
    }
}
