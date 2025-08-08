package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents a non-streaming JSON-RPC request in the A2A protocol.
 * This abstract sealed class serves as the base for all JSON-RPC requests that follow
 * a traditional request-response pattern without streaming capabilities.
 *
 * <p>Non-streaming requests are used for operations that:</p>
 * <ul>
 *   <li>Expect a single, complete response</li>
 *   <li>Do not require real-time updates or incremental data delivery</li>
 *   <li>Follow the standard JSON-RPC 2.0 request-response model</li>
 * </ul>
 *
 * <p>This class is sealed and permits only the following concrete implementations:</p>
 * <ul>
 *   <li>{@link GetTaskRequest} - for retrieving task information</li>
 *   <li>{@link CancelTaskRequest} - for canceling running tasks</li>
 *   <li>{@link SetTaskPushNotificationConfigRequest} - for configuring push notifications</li>
 *   <li>{@link GetTaskPushNotificationConfigRequest} - for retrieving push notification config</li>
 *   <li>{@link SendMessageRequest} - for sending messages without streaming</li>
 * </ul>
 *
 * <p>The class uses a custom deserializer ({@link NonStreamingJSONRPCRequestDeserializer})
 * to properly handle the polymorphic deserialization based on the method field.</p>
 *
 * @param <T> the type of the request parameters
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = NonStreamingJSONRPCRequestDeserializer.class)
public abstract sealed class NonStreamingJSONRPCRequest<T> extends JSONRPCRequest<T> permits GetTaskRequest,
        CancelTaskRequest, SetTaskPushNotificationConfigRequest, GetTaskPushNotificationConfigRequest,
        SendMessageRequest, DeleteTaskPushNotificationConfigRequest, ListTaskPushNotificationConfigRequest {
}
