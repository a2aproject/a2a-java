package io.a2a.extras.opentelemetry.it;

import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.tasks.AgentEmitter;
import io.a2a.spec.A2AError;
import io.a2a.spec.TextPart;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Simple AgentExecutor for integration testing.
 * Echoes back the user's message and completes the task immediately.
 */
@ApplicationScoped
public class SimpleAgentExecutor implements AgentExecutor {

  private final Tracer tracer;

  public SimpleAgentExecutor(Tracer tracer) {
    this.tracer = tracer;
  }

    @Override
    public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
        // If task doesn't exist, create it
        if (context.getTask() == null) {
            emitter.submit();
        }

        // Get the user's message
        String userText = context.getMessage().parts().stream()
                .filter(part -> part instanceof TextPart)
                .map(part -> ((TextPart) part).text())
                .findFirst()
                .orElse("");

        tracer.spanBuilder("SimpleAgentExecutor.execute")
                .setAttribute("user.input.length", userText.length())
                .startSpan()
                .end();
        // Echo it back
        String response = "Echo: " + userText;
        emitter.complete(A2A.toAgentMessage(response));
    }

    @Override
    public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
        emitter.cancel();
    }
}
