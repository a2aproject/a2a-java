package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class A2AErrorCodesTest {
    @Test
    void mapsA2aErrorsAccordingToVersion100() {
        assertEquals(409, A2AErrorCodes.TASK_NOT_CANCELABLE.httpCode());
        assertEquals("UNIMPLEMENTED", A2AErrorCodes.PUSH_NOTIFICATION_NOT_SUPPORTED.grpcStatus());
        assertEquals("UNIMPLEMENTED", A2AErrorCodes.UNSUPPORTED_OPERATION.grpcStatus());
        assertEquals(415, A2AErrorCodes.CONTENT_TYPE_NOT_SUPPORTED.httpCode());
        assertEquals(502, A2AErrorCodes.INVALID_AGENT_RESPONSE.httpCode());
        assertEquals("UNIMPLEMENTED", A2AErrorCodes.VERSION_NOT_SUPPORTED.grpcStatus());
    }
}
