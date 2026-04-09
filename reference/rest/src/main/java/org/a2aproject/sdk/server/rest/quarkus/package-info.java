/**
 * Quarkus REST reference implementation for the A2A protocol.
 *
 * <p>This package provides a ready-to-use Quarkus-based REST transport implementation
 * that maps HTTP endpoints to A2A protocol operations. It serves as both a reference
 * implementation and a production-ready solution for deploying A2A agents over HTTP/REST.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link A2AServerRoutes} - Vert.x route definitions mapping HTTP paths to A2A operations</li>
 *   <li>{@link CallContextFactory} - Extensible factory for creating {@link org.a2aproject.sdk.server.ServerCallContext}</li>
 *   <li>{@link QuarkusRestTransportMetadata} - Transport protocol metadata implementation</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <pre>
 * HTTP Request (Quarkus/Vert.x)
 *     ↓
 * A2AServerRoutes (@Route methods)
 *     ↓
 * RestHandler (transport/rest)
 *     ↓
 * RequestHandler (server-common)
 *     ↓
 * AgentExecutor (user implementation)
 * </pre>
 *
 * <h2>Supported Endpoints</h2>
 * <ul>
 *   <li>{@code POST /message:send} - Send message (blocking)</li>
 *   <li>{@code POST /message:stream} - Send message (streaming SSE)</li>
 *   <li>{@code GET /tasks} - List tasks</li>
 *   <li>{@code GET /tasks/{taskId}} - Get task by ID</li>
 *   <li>{@code POST /tasks/{taskId}:cancel} - Cancel task</li>
 *   <li>{@code POST /tasks/{taskId}:subscribe} - Subscribe to task updates (SSE)</li>
 *   <li>{@code GET /.well-known/agent-card.json} - Get agent card</li>
 * </ul>
 *
 * <h2>Multi-tenancy Support</h2>
 * <p>All endpoints support optional tenant prefixes in the URL path:
 * <ul>
 *   <li>{@code /message:send} - Default tenant (empty string)</li>
 *   <li>{@code /tenant-123/message:send} - Tenant "tenant-123"</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Add this module as a dependency to your Quarkus project:
 * <pre>{@code
 * <dependency>
 *   <groupId>org.a2aproject.sdk</groupId>
 *   <artifactId>a2a-java-sdk-reference-rest</artifactId>
 *   <version>${a2a.version}</version>
 * </dependency>
 * }</pre>
 *
 * <p>Provide CDI beans for {@link org.a2aproject.sdk.spec.AgentCard} and
 * {@link org.a2aproject.sdk.server.agentexecution.AgentExecutor}, and the REST endpoints
 * will be automatically registered.
 *
 * @see org.a2aproject.sdk.transport.rest.handler
 * @see org.a2aproject.sdk.server.requesthandlers
 */
@NullMarked
package org.a2aproject.sdk.server.rest.quarkus;

import org.jspecify.annotations.NullMarked;

