package org.a2aproject.sdk.examples.springboot.rest.server;

import java.util.List;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringBootRestServerAgentExecutor implements AgentExecutor {

    @Override
    public void execute(RequestContext context, AgentEmitter agentEmitter) {
        String input = context.getUserInput("\n");
        log.info("AgentExecutor received input: {}", input);

        if (input != null && input.toLowerCase().contains("stream")) {
            log.info("Running streaming task demo");
            agentEmitter.submit();
            agentEmitter.startWork();
            agentEmitter.addArtifact(List.of(new TextPart("Streaming artifact from Spring Boot REST")));
            agentEmitter.complete();
            return;
        }

        log.info("Returning direct message response");
        agentEmitter.sendMessage("Hello from Spring Boot REST");
    }

    @Override
    public void cancel(RequestContext context, AgentEmitter agentEmitter) {
        log.info("Cancel requested for task {}", context.getTaskId());
        throw new UnsupportedOperationError();
    }
}
