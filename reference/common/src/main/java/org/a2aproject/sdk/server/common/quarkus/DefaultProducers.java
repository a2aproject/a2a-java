package org.a2aproject.sdk.server.common.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import io.quarkus.arc.DefaultBean;

/**
 * Contains beans annotated with the Quarkus @DefaultBean annotation, in order to avoid
 * injection failures when building the Quarkus application as discussed in
 * <a href="https://github.com/a2aproject/a2a-java/issues/213">Issue 213</a>.
 *
 * If an application provides actual implementations of these beans,
 * those will be used instead.
 */
@ApplicationScoped
public class DefaultProducers {

    @Produces
    @DefaultBean
    public AgentExecutor createDefaultAgentExecutor() {
        throw new IllegalStateException(wrap("Please provide your own AgentExecutor implementation"));
    }

    private String wrap(String s) {
        return s +
                " as a CDI bean. Your bean will automatically take precedence over this @DefaultBean " +
                "annotated implementation.";
    }
}
