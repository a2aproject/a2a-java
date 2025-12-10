package io.a2a.spec;

/**
 * All the error codes for A2A errors.
 */
public interface A2AErrorCodes {

    /** Error code indicating the requested task was not found (-32001). */
    int TASK_NOT_FOUND_ERROR_CODE = -32001;

    /** Error code indicating the task cannot be canceled in its current state (-32002). */
    int TASK_NOT_CANCELABLE_ERROR_CODE = -32002;

    /** Error code indicating push notifications are not supported by this agent (-32003). */
    int PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE = -32003;

    /** Error code indicating the requested operation is not supported (-32004). */
    int UNSUPPORTED_OPERATION_ERROR_CODE = -32004;

    /** Error code indicating the content type is not supported (-32005). */
    int CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE = -32005;

    /** Error code indicating the agent returned an invalid response (-32006). */
    int INVALID_AGENT_RESPONSE_ERROR_CODE = -32006;

    /** Error code indicating authenticated extended card is not configured (-32007). */
    int AUTHENTICATED_EXTENDED_CARD_NOT_CONFIGURED_ERROR_CODE = -32007;

    /** JSON-RPC error code for invalid request structure (-32600). */
    int INVALID_REQUEST_ERROR_CODE = -32600;

    /** JSON-RPC error code for method not found (-32601). */
    int METHOD_NOT_FOUND_ERROR_CODE = -32601;

    /** JSON-RPC error code for invalid method parameters (-32602). */
    int INVALID_PARAMS_ERROR_CODE = -32602;

    /** JSON-RPC error code for internal server errors (-32603). */
    int INTERNAL_ERROR_CODE = -32603;

    /** JSON-RPC error code for JSON parsing errors (-32700). */
    int JSON_PARSE_ERROR_CODE = -32700;
}
