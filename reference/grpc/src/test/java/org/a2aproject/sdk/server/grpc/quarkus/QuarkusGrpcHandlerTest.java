package org.a2aproject.sdk.server.grpc.quarkus;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.Executor;

import jakarta.enterprise.inject.Instance;

import org.a2aproject.sdk.server.grpc.quarkus.registry.GrpcAgent;
import org.a2aproject.sdk.server.grpc.quarkus.registry.MultiAgentRegistry;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.transport.grpc.context.GrpcContextKeys;
import io.grpc.Context;
import io.grpc.Metadata;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link QuarkusGrpcHandler}'s multi-agent dispatch logic: selecting the
 * agent to serve a call based on the {@code X-A2A-Agent-Id} metadata header, with fallback
 * to the default single-agent beans.
 */
public class QuarkusGrpcHandlerTest {

    private static final Metadata.Key<String> AGENT_ID_KEY =
            Metadata.Key.of("x-a2a-agent-id", Metadata.ASCII_STRING_MARSHALLER);

    @SuppressWarnings("unchecked")
    private Instance<AgentCard> instanceOf(AgentCard value) {
        Instance<AgentCard> instance = mock(Instance.class);
        when(instance.isResolvable()).thenReturn(value != null);
        when(instance.get()).thenReturn(value);
        return instance;
    }

    @SuppressWarnings("unchecked")
    private Instance<RequestHandler> instanceOf(RequestHandler value) {
        Instance<RequestHandler> instance = mock(Instance.class);
        when(instance.isResolvable()).thenReturn(value != null);
        when(instance.get()).thenReturn(value);
        return instance;
    }

    @SuppressWarnings("unchecked")
    private Instance<MultiAgentRegistry> instanceOf(MultiAgentRegistry value) {
        Instance<MultiAgentRegistry> instance = mock(Instance.class);
        when(instance.isResolvable()).thenReturn(value != null);
        when(instance.get()).thenReturn(value);
        return instance;
    }

    @SuppressWarnings("unchecked")
    private <T> T runWithAgentIdHeader(String agentId, java.util.function.Supplier<T> action) {
        Metadata metadata = new Metadata();
        if (agentId != null) {
            metadata.put(AGENT_ID_KEY, agentId);
        }
        Context context = Context.current().withValue(GrpcContextKeys.METADATA_KEY, metadata);
        Context previous = context.attach();
        try {
            return action.get();
        } finally {
            context.detach(previous);
        }
    }

    @Test
    public void testMultiAgentMode_DispatchesByAgentIdHeader() {
        AgentCard agentACard = mock(AgentCard.class);
        RequestHandler agentARequestHandler = mock(RequestHandler.class);
        GrpcAgent agentA = new GrpcAgent(agentACard, null, agentARequestHandler);

        AgentCard agentBCard = mock(AgentCard.class);
        RequestHandler agentBRequestHandler = mock(RequestHandler.class);
        GrpcAgent agentB = new GrpcAgent(agentBCard, null, agentBRequestHandler);

        MultiAgentRegistry registry = mock(MultiAgentRegistry.class);
        when(registry.getAgents()).thenReturn(Map.of("agentA", agentA, "agentB", agentB));

        QuarkusGrpcHandler handler = new QuarkusGrpcHandler(
                instanceOf((AgentCard) null),
                instanceOf((AgentCard) null),
                instanceOf((RequestHandler) null),
                mock(Instance.class),
                instanceOf(registry),
                mock(Executor.class));

        runWithAgentIdHeader("agentB", () -> {
            assertSame(agentBRequestHandler, handler.getRequestHandler());
            assertSame(agentBCard, handler.getAgentCard());
            return null;
        });
    }

    @Test
    public void testMultiAgentMode_UnknownAgentId_FallsBackToDefaultHandler() {
        AgentCard defaultCard = mock(AgentCard.class);
        RequestHandler defaultRequestHandler = mock(RequestHandler.class);

        MultiAgentRegistry registry = mock(MultiAgentRegistry.class);
        when(registry.getAgents()).thenReturn(Map.of());

        QuarkusGrpcHandler handler = new QuarkusGrpcHandler(
                instanceOf(defaultCard),
                instanceOf((AgentCard) null),
                instanceOf(defaultRequestHandler),
                mock(Instance.class),
                instanceOf(registry),
                mock(Executor.class));

        runWithAgentIdHeader("unknown-agent", () -> {
            assertSame(defaultRequestHandler, handler.getRequestHandler());
            assertSame(defaultCard, handler.getAgentCard());
            return null;
        });
    }

    @Test
    public void testMultiAgentMode_UnknownAgentId_NoDefaultHandler_Throws() {
        MultiAgentRegistry registry = mock(MultiAgentRegistry.class);
        when(registry.getAgents()).thenReturn(Map.of());

        QuarkusGrpcHandler handler = new QuarkusGrpcHandler(
                instanceOf((AgentCard) null),
                instanceOf((AgentCard) null),
                instanceOf((RequestHandler) null),
                mock(Instance.class),
                instanceOf(registry),
                mock(Executor.class));

        runWithAgentIdHeader("unknown-agent", () ->
                assertThrows(InvalidRequestError.class, handler::getRequestHandler));
    }

    @Test
    public void testSingleAgentMode_NoRegistry_UsesDefaultHandler() {
        AgentCard defaultCard = mock(AgentCard.class);
        RequestHandler defaultRequestHandler = mock(RequestHandler.class);

        QuarkusGrpcHandler handler = new QuarkusGrpcHandler(
                instanceOf(defaultCard),
                instanceOf((AgentCard) null),
                instanceOf(defaultRequestHandler),
                mock(Instance.class),
                instanceOf((MultiAgentRegistry) null),
                mock(Executor.class));

        assertSame(defaultRequestHandler, handler.getRequestHandler());
        assertSame(defaultCard, handler.getAgentCard());
    }
}
