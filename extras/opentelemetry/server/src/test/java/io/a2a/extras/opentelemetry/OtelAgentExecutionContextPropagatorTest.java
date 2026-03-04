package io.a2a.extras.opentelemetry;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OtelAgentExecutionContextPropagatorTest {

  ContextKey<String> exampleContextKey = ContextKey.named("test-key");
  Scope scope = null;

  @BeforeEach
  void setup() {
    Context context = Context.current().with(exampleContextKey, "test-value");
    scope = context.makeCurrent();
  }

  @Test
  void testOtelAgentExecutionContextPropagation() throws InterruptedException {

    OtelAgentExecutionContextPropagator propagator = new OtelAgentExecutionContextPropagator();
    Runnable wrappedContext =
        propagator.wrap(() -> assertEquals("test-value", Context.current().get(exampleContextKey)));

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    CompletableFuture<Void> future = CompletableFuture.runAsync(wrappedContext, executorService);
    future.join();
    executorService.shutdown();
    executorService.awaitTermination(5, TimeUnit.SECONDS);
  }

  @AfterEach
  void tearDown() {
    scope.close();
  }
}