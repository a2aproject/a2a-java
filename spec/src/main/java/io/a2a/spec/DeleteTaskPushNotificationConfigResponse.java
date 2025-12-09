package io.a2a.spec;

/**
 * JSON-RPC response confirming deletion of a task's push notification configuration.
 * <p>
 * This response confirms successful deletion of the push notification configuration.
 * The result is void (no content) on success.
 * <p>
 * If an error occurs during deletion, the error field will contain a {@link JSONRPCError}.
 *
 * @see DeleteTaskPushNotificationConfigRequest for the corresponding request
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class DeleteTaskPushNotificationConfigResponse extends JSONRPCResponse<Void> {

    public DeleteTaskPushNotificationConfigResponse(String jsonrpc, Object id, Void result,JSONRPCError error) {
        super(jsonrpc, id, result, error, Void.class);
    }

    public DeleteTaskPushNotificationConfigResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public DeleteTaskPushNotificationConfigResponse(Object id) {
        this(null, id, null, null);
    }

}
