package org.a2aproject.sdk.server.grpc.quarkus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.inject.Instance;

import org.a2aproject.sdk.server.FixedInstance;
import org.a2aproject.sdk.server.TestInstances;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.transport.grpc.handler.CallContextFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link QuarkusGrpcHandler} does not eagerly resolve {@link Instance} parameters
 * during construction. This guards against the regression described in
 * <a href="https://github.com/a2aproject/a2a-java/issues/1033">issue #1033</a>, where eager
 * resolution prevented startup when the AgentCard producer depended on the HTTP server address.
 */
class QuarkusGrpcHandlerLazyResolutionTest {

    @Test
    void constructorDoesNotResolveAgentCardInstances() {
        Instance<AgentCard> throwOnGet = TestInstances.throwOnGet();

        assertDoesNotThrow(() -> new QuarkusGrpcHandler(
                throwOnGet,
                throwOnGet,
                null,
                FixedInstance.empty(),
                Runnable::run,
                null));
    }

    @Test
    void getExtendedAgentCardResolvesOnAccess() {
        AtomicBoolean resolved = new AtomicBoolean(false);
        AgentCard expected = AgentCard.builder()
                .name("extended").description("test").version("1.0.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("gRPC", "http://localhost:9999")))
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(new ArrayList<>())
                .defaultOutputModes(new ArrayList<>())
                .skills(new ArrayList<>()).build();

        Instance<AgentCard> trackingInstance = new FixedInstance<>(expected) {
            @Override
            public AgentCard get() {
                resolved.set(true);
                return super.get();
            }
        };

        QuarkusGrpcHandler handler = new QuarkusGrpcHandler(
                FixedInstance.empty(),
                trackingInstance,
                null,
                FixedInstance.empty(),
                Runnable::run,
                null);

        assertFalse(resolved.get(), "should not resolve during construction");
        AgentCard result = handler.getExtendedAgentCard();
        assertTrue(resolved.get(), "should resolve on access");
        assertEquals(expected, result);
    }
}
