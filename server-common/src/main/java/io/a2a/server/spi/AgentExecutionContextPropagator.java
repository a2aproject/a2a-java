package io.a2a.server.spi;

/**
 * Interface for propagating the agent execution context across different threads or asynchronous operations.
 * Implementations can wrap Runnable tasks to ensure that the context is properly propagated.
 */
public interface AgentExecutionContextPropagator {

  /**
   * Wraps a Runnable task to propagate the agent execution context when the task is executed.
   * @param runnable the Runnable task to wrap
   */
  Runnable wrap(Runnable runnable);
}
