package io.a2a.server.context;

import io.a2a.server.spi.AgentExecutionContextPropagator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DefaultAgentExecutionContextPropagator implements AgentExecutionContextPropagator {

  @Override
  public Runnable wrap(Runnable runnable) {
    return runnable;
  }
}
