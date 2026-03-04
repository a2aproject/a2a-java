package io.a2a.server.grpc.quarkus;

import java.util.concurrent.Executor;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.a2a.server.ExtendedAgentCard;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.AgentCard;
import io.a2a.transport.grpc.handler.CallContextFactory;
import io.a2a.transport.grpc.handler.GrpcHandler;
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.RegisterInterceptor;
import io.quarkus.security.Authenticated;
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
 * @see io.a2a.transport.grpc.handler.GrpcHandler
 * @see A2AExtensionsInterceptor
 * @see CallContextFactory
 */
@GrpcService
@RegisterInterceptor(A2AExtensionsInterceptor.class)
@Authenticated
public class QuarkusGrpcHandler extends GrpcHandler {

    private final AgentCard agentCard;
    private final AgentCard extendedAgentCard;
    private final RequestHandler requestHandler;
    private final Instance<CallContextFactory> callContextFactoryInstance;
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
     * @param agentCard the public agent card (qualified with {@code @PublicAgentCard})
     * @param extendedAgentCard the extended agent card instance (qualified with {@code @ExtendedAgentCard})
     * @param requestHandler the request handler for protocol operations
     * @param callContextFactoryInstance the call context factory instance (optional)
     * @param executor the executor for async operations (qualified with {@code @Internal})
     */
    @Inject
    public QuarkusGrpcHandler(@PublicAgentCard AgentCard agentCard,
                              @ExtendedAgentCard Instance<AgentCard> extendedAgentCard,
                              RequestHandler requestHandler,
                              Instance<CallContextFactory> callContextFactoryInstance,
                              @Internal Executor executor) {
        this.agentCard = agentCard;
        if (extendedAgentCard != null && extendedAgentCard.isResolvable()) {
            this.extendedAgentCard = extendedAgentCard.get();
        } else {
            this.extendedAgentCard = null;
        }
        this.requestHandler = requestHandler;
        this.callContextFactoryInstance = callContextFactoryInstance;
        this.executor = executor;
    }

    @Override
    protected RequestHandler getRequestHandler() {
        return requestHandler;
    }

    @Override
    protected AgentCard getAgentCard() {
        return agentCard;
    }

    @Override
    protected AgentCard getExtendedAgentCard() {
        return extendedAgentCard;
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
