package io.a2a.extras.opentelemetry;

import io.a2a.server.spi.AgentExecutionContextPropagator;
import io.opentelemetry.context.Context;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * An implementation of AgentExecutionContextPropagator that uses OpenTelemetry's Context to propagate the execution
 * context. This allows for proper context propagation across threads and asynchronous operations when using
 * OpenTelemetry for tracing. The implementation simply delegates the wrapping OpenTelemetry's context propagation
 * mechanism.
 *
 * @see Context#wrap(Runnable)
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class OtelAgentExecutionContextPropagator implements AgentExecutionContextPropagator {
  @Override
  public Runnable wrap(Runnable runnable) {
    return Context.current().wrap(runnable);
  }
}
