/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * gRPC transport handler implementations for the A2A protocol.
 *
 * <p>This package contains the core gRPC handler that processes gRPC requests
 * and translates them to A2A protocol operations. It supports both unary (blocking)
 * and streaming responses with proper gRPC error handling and status codes.
 *
 * <h2>gRPC Protocol</h2>
 * <p>This implementation uses Protocol Buffers for message serialization and provides:
 * <ul>
 *   <li>Unary RPC calls for blocking operations</li>
 *   <li>Server streaming RPC for streaming responses</li>
 *   <li>Rich error handling with gRPC status codes</li>
 *   <li>Context-aware metadata extraction</li>
 * </ul>
 *
 * <h2>Supported Methods</h2>
 * <ul>
 *   <li>{@code SendMessage} - Send message (unary/blocking)</li>
 *   <li>{@code SendStreamingMessage} - Send message (server streaming)</li>
 *   <li>{@code SubscribeToTask} - Subscribe to task updates (server streaming)</li>
 *   <li>{@code GetTask} - Get task by ID</li>
 *   <li>{@code ListTasks} - List tasks with filtering</li>
 *   <li>{@code CancelTask} - Cancel task execution</li>
 *   <li>{@code GetTaskPushNotificationConfig} - Get push notification config</li>
 *   <li>{@code CreateTaskPushNotificationConfig} - Create push notification config</li>
 *   <li>{@code ListTaskPushNotificationConfig} - List push notification configs</li>
 *   <li>{@code DeleteTaskPushNotificationConfig} - Delete push notification config</li>
 *   <li>{@code GetExtendedAgentCard} - Get extended agent card</li>
 * </ul>
 *
 * <h2>Context Access</h2>
 * <p>The gRPC handler provides rich context information equivalent to Python's
 * {@code grpc.aio.ServicerContext}, including:
 * <ul>
 *   <li>Request metadata (headers)</li>
 *   <li>Method name</li>
 *   <li>Peer information</li>
 *   <li>A2A protocol version and extensions</li>
 * </ul>
 *
 * @see io.a2a.transport.grpc.handler.GrpcHandler
 * @see io.a2a.transport.grpc.context.GrpcContextKeys
 */
@NullMarked
package io.a2a.transport.grpc.handler;

import org.jspecify.annotations.NullMarked;

