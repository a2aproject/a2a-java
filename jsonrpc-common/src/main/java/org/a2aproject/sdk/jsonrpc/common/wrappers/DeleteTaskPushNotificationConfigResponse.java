package org.a2aproject.sdk.jsonrpc.common.wrappers;

import org.a2aproject.sdk.spec.A2AError;

/**
 * JSON-RPC response confirming deletion of a task's push notification configuration.
 * <p>
 * This response confirms successful deletion of the push notification configuration.
 * The result is void (no content) on success.
 * <p>
 * If an error occurs during deletion, the error field will contain a {@link A2AError}.
 *
 * @see DeleteTaskPushNotificationConfigRequest for the corresponding request
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class DeleteTaskPushNotificationConfigResponse extends A2AResponse<Void> {

    /**
     * Creates a new DeleteTaskPushNotificationConfigResponse with full JSON-RPC parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the response identifier matching the request
     * @param result the result (always null/Void for this response type)
     * @param error the error if the request failed, null on success
     */
    public DeleteTaskPushNotificationConfigResponse(String jsonrpc, Object id, Void result,A2AError error) {
        super(jsonrpc, id, result, error, Void.class);
    }

    /**
     * Creates a new error DeleteTaskPushNotificationConfigResponse with default JSON-RPC version.
     *
     * @param id the response identifier matching the request
     * @param error the error describing why the deletion failed
     */
    public DeleteTaskPushNotificationConfigResponse(Object id, A2AError error) {
        this(null, id, null, error);
    }

    /**
     * Creates a new successful DeleteTaskPushNotificationConfigResponse with default JSON-RPC version.
     *
     * @param id the response identifier matching the request
     */
    public DeleteTaskPushNotificationConfigResponse(Object id) {
        this(null, id, null, null);
    }

}
