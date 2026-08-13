package org.a2aproject.sdk.server.grpc.quarkus;

import static java.util.Locale.ROOT;

import java.util.concurrent.Executor;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.grpc.quarkus.registry.GrpcAgent;
import org.a2aproject.sdk.server.grpc.quarkus.registry.MultiAgentRegistry;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.util.async.Internal;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.transport.grpc.context.GrpcContextKeys;
import org.a2aproject.sdk.transport.grpc.handler.CallContextFactory;
import org.a2aproject.sdk.transport.grpc.handler.GrpcHandler;
import io.grpc.Context;
import io.grpc.Metadata;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.RegisterInterceptor;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import org.jspecify.annotations.Nullable;

/**
 * Quarkus gRPC service implementation for the A2A protocol.
 *
 * <p>This class provides a production-ready gRPC service built on Quarkus gRPC,
 * implementing the A2A protocol with CDI integration, authentication, and
 * interceptor support for metadata extraction.
 *
 * <h2>CDI Integration</h2>
 * <p>This class is a Quarkus gRPC service ({@code @GrpcService}) that automatically:
 * <ul>
 *   <li>Injects the public {@link AgentCard} (required)</li>
 *   <li>Injects the extended {@link AgentCard} (optional)</li>
 *   <li>Injects the {@link RequestHandler} for protocol operations</li>
 *   <li>Injects the {@link CallContextFactory} for custom context creation (optional)</li>
 *   <li>Injects the {@link Executor} for async operations</li>
 * </ul>
 *
 * <h2>Security</h2>
 * <p>The service is protected with {@code @Authenticated} annotation, requiring
 * authentication for all gRPC method calls. Configure authentication in
 * {@code application.properties}:
 * <pre>
 * quarkus.security.users.embedded.enabled=true
 * quarkus.security.users.embedded.plain-text=true
 * quarkus.security.users.embedded.users.alice=password
 * </pre>
 *
 * <h2>Interceptor Registration</h2>
 * <p>The {@code @RegisterInterceptor} annotation automatically registers
 * {@link A2AExtensionsInterceptor} to capture A2A protocol headers and
 * metadata before service methods are invoked.
 *
 * <h2>Extension Points</h2>
 * <p>To customize context creation, provide a CDI bean implementing
 * {@link CallContextFactory}:
 * <pre>{@code
 * @ApplicationScoped
 * public class CustomCallContextFactory implements CallContextFactory {
 *     @Override
 *     public <V> ServerCallContext create(StreamObserver<V> responseObserver) {
 *         // Custom context creation logic
 *     }
 * }
 * }</pre>
 *
 * @see org.a2aproject.sdk.transport.grpc.handler.GrpcHandler
 * @see A2AExtensionsInterceptor
 * @see CallContextFactory
 */
@GrpcService
@RegisterInterceptor(A2AExtensionsInterceptor.class)
@RegisterInterceptor(BlockingOffloadInterceptor.class)
@Authenticated
@Blocking
public class QuarkusGrpcHandler extends GrpcHandler {

    private static final Metadata.Key<String> AGENT_ID_KEY =
            Metadata.Key.of(A2AHeaders.X_A2A_AGENT_ID.toLowerCase(ROOT), Metadata.ASCII_STRING_MARSHALLER);

    private final Instance<AgentCard> agentCardInstance;
    private final Instance<AgentCard> extendedAgentCardInstance;
    private final Instance<RequestHandler> requestHandlerInstance;
    private final Instance<CallContextFactory> callContextFactoryInstance;
    private final Instance<MultiAgentRegistry> multiAgentRegistryInstance;
    private final Executor executor;

    /**
     * Constructs a new QuarkusGrpcHandler with CDI-injected dependencies.
     *
     * <p>This constructor is invoked by CDI to create the gRPC service bean,
     * injecting all required and optional dependencies.
     *
     * <p><b>Required Dependencies:</b>
     * <ul>
     *   <li>{@code agentCard} - Public agent card defining capabilities</li>
     *   <li>{@code requestHandler} - Request handler for protocol operations</li>
     *   <li>{@code executor} - Executor for async operations</li>
     * </ul>
     *
     * <p><b>Optional Dependencies:</b>
     * <ul>
     *   <li>{@code extendedAgentCard} - Extended agent card (can be unresolvable)</li>
     *   <li>{@code callContextFactoryInstance} - Custom context factory (can be unsatisfied)</li>
     * </ul>
     *
     * @param agentCard the public agent card instance (qualified with {@code @PublicAgentCard}); may be
     *     unresolvable when every agent is served through a {@link MultiAgentRegistry}
     * @param extendedAgentCard the extended agent card instance (qualified with {@code @ExtendedAgentCard})
     * @param requestHandler the request handler instance for protocol operations; may be unresolvable
     *     when every agent is served through a {@link MultiAgentRegistry}
     * @param callContextFactoryInstance the call context factory instance (optional)
     * @param multiAgentRegistryInstance the multi-agent registry instance (optional)
     * @param executor the executor for async operations (qualified with {@code @Internal})
     */
    @Inject
    public QuarkusGrpcHandler(@PublicAgentCard Instance<AgentCard> agentCard,
                              @ExtendedAgentCard Instance<AgentCard> extendedAgentCard,
                              Instance<RequestHandler> requestHandler,
                              Instance<CallContextFactory> callContextFactoryInstance,
                              Instance<MultiAgentRegistry> multiAgentRegistryInstance,
                              @Internal Executor executor) {
        this.agentCardInstance = agentCard;
        this.extendedAgentCardInstance = extendedAgentCard;
        this.requestHandlerInstance = requestHandler;
        this.callContextFactoryInstance = callContextFactoryInstance;
        this.multiAgentRegistryInstance = multiAgentRegistryInstance;
        this.executor = executor;
    }

    @Override
    protected RequestHandler getRequestHandler() {
        return resolveAgent().requestHandler();
    }

    @Override
    protected AgentCard getAgentCard() {
        return resolveAgent().agentCard();
    }

    @Override
    protected AgentCard getExtendedAgentCard() {
        return resolveAgent().extendedAgentCard();
    }

    /**
     * Resolves the agent for the current call: by {@code X-A2A-Agent-Id} header via
     * {@link MultiAgentRegistry} if present, else the default single-agent beans.
     *
     * @throws InvalidRequestError if no agent could be resolved for this call
     */
    private GrpcAgent resolveAgent() {
        if (multiAgentRegistryInstance.isResolvable()) {
            String agentId = currentAgentId();
            GrpcAgent agent = agentId != null ? multiAgentRegistryInstance.get().getAgents().get(agentId) : null;
            if (agent != null) {
                return agent;
            }
        }
        if (agentCardInstance.isResolvable() && requestHandlerInstance.isResolvable()) {
            AgentCard extendedAgentCard = extendedAgentCardInstance.isResolvable() ? extendedAgentCardInstance.get() : null;
            return new GrpcAgent(agentCardInstance.get(), extendedAgentCard, requestHandlerInstance.get());
        }
        throw new InvalidRequestError("No agent configured for this request");
    }

    /** @return the {@code X-A2A-Agent-Id} header from the current call's metadata, or null */
    private @Nullable String currentAgentId() {
        Metadata metadata = GrpcContextKeys.METADATA_KEY.get(Context.current());
        return metadata != null ? metadata.get(AGENT_ID_KEY) : null;
    }

    @Override
    protected CallContextFactory getCallContextFactory() {
        return callContextFactoryInstance.isUnsatisfied() ? null : callContextFactoryInstance.get();
    }

    @Override
    protected Executor getExecutor() {
        return executor;
    }
}
