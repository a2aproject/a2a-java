/**
 * Quarkus gRPC reference implementation for the A2A protocol.
 *
 * <p>This package provides a production-ready gRPC server implementation built on
 * Quarkus gRPC and Protocol Buffers, demonstrating best practices for A2A protocol
 * integration with CDI, authentication, and interceptor support.
 *
 * <h2>Architecture</h2>
 * <pre>
 * gRPC Request (Protocol Buffers)
 *     ↓
 * A2AExtensionsInterceptor (metadata extraction)
 *     ↓
 * QuarkusGrpcHandler (@GrpcService)
 *     ├─ Protobuf → Domain conversion
 *     ├─ Create ServerCallContext
 *     ├─ Route to GrpcHandler (transport layer)
 *     └─ Domain → Protobuf conversion
 *         ↓
 * GrpcHandler (transport/grpc)
 *     ↓
 * RequestHandler (server-common)
 *     ↓
 * AgentExecutor (your implementation)
 * </pre>
 *
 * <h2>Core Components</h2>
 * <ul>
 *   <li>{@link io.a2a.server.grpc.quarkus.QuarkusGrpcHandler QuarkusGrpcHandler} - Main gRPC service implementation</li>
 *   <li>{@link io.a2a.server.grpc.quarkus.A2AExtensionsInterceptor A2AExtensionsInterceptor} - Metadata extraction interceptor</li>
 *   <li>{@link io.a2a.server.grpc.quarkus.QuarkusGrpcTransportMetadata QuarkusGrpcTransportMetadata} - Transport protocol identification</li>
 * </ul>
 *
 * <h2>gRPC Methods</h2>
 *
 * <p><b>Unary RPC (blocking):</b>
 * <ul>
 *   <li>{@code SendMessage} - Send message and wait for completion</li>
 *   <li>{@code GetTask} - Get task by ID</li>
 *   <li>{@code ListTasks} - List tasks with filtering</li>
 *   <li>{@code CancelTask} - Cancel task execution</li>
 *   <li>{@code CreateTaskPushNotificationConfig} - Configure push notifications</li>
 *   <li>{@code GetTaskPushNotificationConfig} - Get push notification config</li>
 *   <li>{@code ListTaskPushNotificationConfig} - List push notification configs</li>
 *   <li>{@code DeleteTaskPushNotificationConfig} - Delete push notification config</li>
 *   <li>{@code GetExtendedAgentCard} - Get extended agent card</li>
 * </ul>
 *
 * <p><b>Server Streaming RPC:</b>
 * <ul>
 *   <li>{@code SendStreamingMessage} - Send message with streaming response</li>
 *   <li>{@code SubscribeToTask} - Subscribe to task events</li>
 * </ul>
 *
 * <h2>CDI Integration</h2>
 *
 * <p><b>Required CDI Beans:</b>
 * <ul>
 *   <li>{@link io.a2a.spec.AgentCard AgentCard} with {@code @PublicAgentCard} qualifier</li>
 *   <li>{@link io.a2a.server.agentexecution.AgentExecutor AgentExecutor} implementation</li>
 * </ul>
 *
 * <p><b>Optional CDI Beans:</b>
 * <ul>
 *   <li>{@link io.a2a.spec.AgentCard AgentCard} with {@code @ExtendedAgentCard} qualifier</li>
 *   <li>{@link io.a2a.transport.grpc.handler.CallContextFactory CallContextFactory} for custom context creation</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <p><b>Add Dependency:</b>
 * <pre>{@code
 * <dependency>
 *   <groupId>io.github.a2asdk</groupId>
 *   <artifactId>a2a-java-sdk-reference-grpc</artifactId>
 *   <version>${a2a.version}</version>
 * </dependency>
 * }</pre>
 *
 * <p><b>Provide Agent Card:</b>
 * <pre>{@code
 * @ApplicationScoped
 * public class MyAgentCardProducer {
 *     @Produces @PublicAgentCard
 *     public AgentCard agentCard() {
 *         return new AgentCard.Builder()
 *             .name("My gRPC Agent")
 *             .description("Agent description")
 *             .url("http://localhost:9090")
 *             .capabilities(new AgentCapabilities.Builder()
 *                 .streaming(true)
 *                 .build())
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * <p><b>Provide Agent Executor:</b>
 * <pre>{@code
 * @ApplicationScoped
 * public class MyAgentExecutorProducer {
 *     @Produces
 *     public AgentExecutor agentExecutor() {
 *         return new MyAgentExecutor();
 *     }
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p><b>gRPC Server:</b>
 * <pre>
 * quarkus.grpc.server.port=9090
 * quarkus.grpc.server.host=0.0.0.0
 * </pre>
 *
 * <p><b>Authentication:</b>
 * <pre>
 * quarkus.security.users.embedded.enabled=true
 * quarkus.security.users.embedded.plain-text=true
 * quarkus.security.users.embedded.users.alice=password
 * </pre>
 *
 * <h2>Customization</h2>
 *
 * <p><b>Custom Context Creation:</b>
 * <p>Provide a CDI bean implementing {@link io.a2a.transport.grpc.handler.CallContextFactory CallContextFactory}:
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
 * <h2>Python Equivalence</h2>
 * <p>This implementation provides equivalent functionality to Python's {@code grpc.aio} server:
 * <ul>
 *   <li>{@code grpc.aio.ServicerContext} → {@link io.grpc.Context} with {@link A2AExtensionsInterceptor}</li>
 *   <li>{@code context.invocation_metadata()} → {@link io.a2a.transport.grpc.context.GrpcContextKeys#METADATA_KEY}</li>
 *   <li>{@code context.method()} → {@link io.a2a.transport.grpc.context.GrpcContextKeys#GRPC_METHOD_NAME_KEY}</li>
 *   <li>{@code context.peer()} → {@link io.a2a.transport.grpc.context.GrpcContextKeys#PEER_INFO_KEY}</li>
 * </ul>
 *
 * @see io.a2a.server.grpc.quarkus.QuarkusGrpcHandler
 * @see io.a2a.server.grpc.quarkus.A2AExtensionsInterceptor
 * @see io.a2a.transport.grpc.handler.GrpcHandler
 * @see io.a2a.transport.grpc.context.GrpcContextKeys
 */
package io.a2a.server.grpc.quarkus;
