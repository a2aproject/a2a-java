package io.a2a.transport.grpc.handler;

import io.a2a.server.ServerCallContext;
import io.grpc.stub.StreamObserver;

/**
 * Factory interface for creating {@link ServerCallContext} from gRPC request context.
 *
 * <p>This interface provides an extension point for customizing how {@link ServerCallContext}
 * instances are created in gRPC applications. The default implementation in {@link GrpcHandler}
 * extracts standard information (user, metadata, headers, peer info, protocol version), but
 * applications can provide their own implementation to add custom context data.
 *
 * <h2>Default Behavior</h2>
 * <p>When no CDI bean implementing this interface is provided, {@link GrpcHandler}
 * creates contexts with:
 * <ul>
 *   <li>User authentication from security context</li>
 *   <li>gRPC metadata (headers)</li>
 *   <li>Method name and peer information</li>
 *   <li>A2A protocol version from {@code X-A2A-Version} header</li>
 *   <li>Required extensions from {@code X-A2A-Extensions} header</li>
 *   <li>Response observer for gRPC streaming</li>
 * </ul>
 *
 * <h2>Custom Implementation Example</h2>
 * <pre>{@code
 * @ApplicationScoped
 * public class CustomCallContextFactory implements CallContextFactory {
 *     @Override
 *     public <V> ServerCallContext create(StreamObserver<V> responseObserver) {
 *         // Extract custom data from gRPC context
 *         Context grpcContext = Context.current();
 *         Metadata metadata = GrpcContextKeys.METADATA_KEY.get(grpcContext);
 *         String orgId = metadata.get(
 *             Metadata.Key.of("x-organization-id", Metadata.ASCII_STRING_MARSHALLER)
 *         );
 *
 *         Map<String, Object> state = new HashMap<>();
 *         state.put("organization", orgId);
 *         state.put("grpc_response_observer", responseObserver);
 *
 *         return new ServerCallContext(
 *             extractUser(),
 *             state,
 *             extractExtensions(grpcContext),
 *             extractVersion(grpcContext)
 *         );
 *     }
 * }
 * }</pre>
 *
 * @see ServerCallContext
 * @see GrpcHandler#createCallContext
 * @see io.a2a.transport.grpc.context.GrpcContextKeys
 */
public interface CallContextFactory {
    /**
     * Creates a {@link ServerCallContext} from gRPC request context.
     *
     * <p>This method is called for each incoming gRPC request to create the context
     * that will be passed to the {@link io.a2a.server.requesthandlers.RequestHandler}
     * and eventually to the {@link io.a2a.server.agentexecution.AgentExecutor}.
     *
     * <p>Implementations should extract information from the current {@link io.grpc.Context}
     * using {@link io.a2a.transport.grpc.context.GrpcContextKeys} to access metadata,
     * method name, peer info, and A2A protocol headers.
     *
     * @param <V> the response type for the gRPC method
     * @param responseObserver the gRPC response stream observer
     * @return a new ServerCallContext with extracted authentication, metadata, and headers
     */
    <V> ServerCallContext create(StreamObserver<V> responseObserver);
}