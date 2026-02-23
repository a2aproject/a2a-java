/**
 * Quarkus JSON-RPC reference implementation for the A2A protocol.
 *
 * <p>This package provides a production-ready JSON-RPC 2.0 server implementation built on
 * Quarkus and Vert.x Web, demonstrating best practices for A2A protocol integration.
 *
 * <h2>Architecture</h2>
 * <pre>
 * HTTP Request (JSON-RPC 2.0)
 *     ↓
 * A2AServerRoutes (@Singleton)
 *     ├─ Parse JSON-RPC request
 *     ├─ Create ServerCallContext (via CallContextFactory)
 *     ├─ Route to JSONRPCHandler
 *     └─ Return JSON or SSE stream
 *         ↓
 * JSONRPCHandler (transport layer)
 *     ↓
 * DefaultRequestHandler (server-common)
 *     ↓
 * AgentExecutor (your implementation)
 * </pre>
 *
 * <h2>Core Components</h2>
 * <ul>
 *   <li>{@link io.a2a.server.apps.quarkus.A2AServerRoutes A2AServerRoutes} - Vert.x Web routing and request handling</li>
 *   <li>{@link io.a2a.server.apps.quarkus.CallContextFactory CallContextFactory} - Extension point for custom context creation</li>
 *   <li>{@link io.a2a.server.apps.quarkus.QuarkusJSONRPCTransportMetadata QuarkusJSONRPCTransportMetadata} - Transport protocol identification</li>
 * </ul>
 *
 * <h2>JSON-RPC Endpoint</h2>
 * <p><b>Main Endpoint:</b> {@code POST /[tenant]}
 * <p><b>Agent Card:</b> {@code GET /.well-known/agent-card.json}
 *
 * <h2>Request Format</h2>
 * <pre>{@code
 * POST /
 * Content-Type: application/json
 *
 * {
 *   "jsonrpc": "2.0",
 *   "id": "req-123",
 *   "method": "sendMessage",
 *   "params": {
 *     "message": {
 *       "parts": [{"text": "Hello"}]
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>Response Formats</h2>
 *
 * <p><b>Non-Streaming (JSON):</b>
 * <pre>{@code
 * HTTP/1.1 200 OK
 * Content-Type: application/json
 *
 * {
 *   "jsonrpc": "2.0",
 *   "id": "req-123",
 *   "result": {
 *     "task": { ... }
 *   }
 * }
 * }</pre>
 *
 * <p><b>Streaming (SSE):</b>
 * <pre>{@code
 * HTTP/1.1 200 OK
 * Content-Type: text/event-stream
 *
 * id: 0
 * data: {"jsonrpc":"2.0","id":"req-123","result":{...}}
 *
 * id: 1
 * data: {"jsonrpc":"2.0","id":"req-123","result":{...}}
 * }</pre>
 *
 * <p><b>Error Response:</b>
 * <pre>{@code
 * HTTP/1.1 200 OK
 * Content-Type: application/json
 *
 * {
 *   "jsonrpc": "2.0",
 *   "id": "req-123",
 *   "error": {
 *     "code": -32602,
 *     "message": "Invalid params"
 *   }
 * }
 * }</pre>
 *
 * <h2>Supported Methods</h2>
 * <ul>
 *   <li>{@code sendMessage} - Send message (blocking)</li>
 *   <li>{@code sendStreamingMessage} - Send message (streaming SSE)</li>
 *   <li>{@code subscribeToTask} - Subscribe to task events (streaming SSE)</li>
 *   <li>{@code getTask} - Get task by ID</li>
 *   <li>{@code listTasks} - List tasks with filtering</li>
 *   <li>{@code cancelTask} - Cancel task execution</li>
 *   <li>{@code getTaskPushNotificationConfig} - Get push notification config</li>
 *   <li>{@code setTaskPushNotificationConfig} - Create push notification config</li>
 *   <li>{@code listTaskPushNotificationConfig} - List push notification configs</li>
 *   <li>{@code deleteTaskPushNotificationConfig} - Delete push notification config</li>
 *   <li>{@code getExtendedAgentCard} - Get extended agent card</li>
 * </ul>
 *
 * <h2>Multi-Tenancy</h2>
 * <p>Tenant identification from request path:
 * <ul>
 *   <li>{@code POST /} → empty tenant</li>
 *   <li>{@code POST /tenant1} → tenant "tenant1"</li>
 *   <li>{@code POST /org/team} → tenant "org/team"</li>
 * </ul>
 *
 * <h2>Customization</h2>
 *
 * <p><b>Custom Context Creation:</b>
 * <p>Provide a CDI bean implementing {@link io.a2a.server.apps.quarkus.CallContextFactory CallContextFactory}:
 * <pre>{@code
 * @ApplicationScoped
 * public class CustomCallContextFactory implements CallContextFactory {
 *     @Override
 *     public ServerCallContext build(RoutingContext rc) {
 *         // Extract custom data from routing context
 *         Map<String, Object> state = new HashMap<>();
 *         state.put("organization", rc.request().getHeader("X-Org-ID"));
 *         return new ServerCallContext(user, state, extensions, version);
 *     }
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <p>No JSON-RPC-specific configuration required. Standard Quarkus HTTP configuration applies:
 * <pre>
 * quarkus.http.port=9999
 * quarkus.http.host=0.0.0.0
 * </pre>
 *
 * <h2>Authentication</h2>
 * <p>Uses Quarkus Security with {@code @Authenticated} annotation on routes.
 * Configure authentication in {@code application.properties}:
 * <pre>
 * quarkus.security.users.embedded.enabled=true
 * quarkus.security.users.embedded.plain-text=true
 * quarkus.security.users.embedded.users.alice=password
 * </pre>
 *
 * @see io.a2a.server.apps.quarkus.A2AServerRoutes
 * @see io.a2a.server.apps.quarkus.CallContextFactory
 * @see io.a2a.transport.jsonrpc.handler.JSONRPCHandler
 */
package io.a2a.server.apps.quarkus;
