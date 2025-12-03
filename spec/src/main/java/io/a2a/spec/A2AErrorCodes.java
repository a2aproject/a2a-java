/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.a2a.spec;

/**
 * All the error codes for A2A errors.
 */
public interface A2AErrorCodes {
    final int CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE = -32005;
    final int INTERNAL_ERROR_CODE = -32603;
    final int INVALID_AGENT_RESPONSE_ERROR_CODE = -32006;
    final int INVALID_PARAMS_ERROR_CODE = -32602;
    final int JSON_PARSE_ERROR_CODE = -32700;
    final int INVALID_REQUEST_ERROR_CODE = -32600;
    final int METHOD_NOT_FOUND_ERROR_CODE = -32601;
    final int PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE = -32003;
    final int UNSUPPORTED_OPERATION_ERROR_CODE = -32004;
    final int TASK_NOT_CANCELABLE_ERROR_CODE = -32002;
    final int TASK_NOT_FOUND_ERROR_CODE = -32001;
    final int AUTHENTICATED_EXTENDED_CARD_NOT_CONFIGURED_ERROR_CODE = -32007;
}
