/**
 * JSON-RPC transport handler implementations for the A2A protocol.
 *
 * <p>This package contains the core JSON-RPC handler that processes JSON-RPC 2.0 requests
 * over HTTP and translates them to A2A protocol operations. It supports both blocking and
 * streaming responses with proper JSON-RPC error handling.
 *
 * <h2>JSON-RPC 2.0 Protocol</h2>
 * <p>This implementation follows the JSON-RPC 2.0 specification, supporting:
 * <ul>
 *   <li>Request/response pairs with unique request IDs</li>
 *   <li>Error responses with code, message, and optional data</li>
 *   <li>Streaming via Server-Sent Events for applicable methods</li>
 * </ul>
 *
 * <h2>Supported Methods</h2>
 * <ul>
 *   <li>{@code sendMessage} - Send message (blocking)</li>
 *   <li>{@code sendStreamingMessage} - Send message (streaming)</li>
 *   <li>{@code subscribeToTask} - Subscribe to task updates (streaming)</li>
 *   <li>{@code getTask} - Get task by ID</li>
 *   <li>{@code listTasks} - List tasks with filtering</li>
 *   <li>{@code cancelTask} - Cancel task execution</li>
 *   <li>{@code getTaskPushNotificationConfig} - Get push notification config</li>
 *   <li>{@code setTaskPushNotificationConfig} - Create push notification config</li>
 *   <li>{@code listTaskPushNotificationConfig} - List push notification configs</li>
 *   <li>{@code deleteTaskPushNotificationConfig} - Delete push notification config</li>
 * </ul>
 *
 * @see io.a2a.transport.jsonrpc.handler.JSONRPCHandler
 */
@NullMarked
package io.a2a.transport.jsonrpc.handler;

import org.jspecify.annotations.NullMarked;

