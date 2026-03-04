package io.a2a.server.context;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultAgentExecutionContextPropagatorTest {

  @Test
  void testDefaultAgentExecutionPropagation() {
    DefaultAgentExecutionContextPropagator propagator = new DefaultAgentExecutionContextPropagator();
    Runnable runnable = () -> {
    };
    Assertions.assertSame(runnable, propagator.wrap(runnable));
  }
}