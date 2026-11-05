package org.a2aproject.sdk.compat03.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.junit.jupiter.api.Test;

public class ThrowableDeserializationSecurityTest {
    private static boolean nonThrowableInitialized;

    public static class NonThrowable {
        static {
            nonThrowableInitialized = true;
        }
    }

    public static class CustomThrowable extends Exception {
        public CustomThrowable(String message) {
            super(message);
        }
    }

    @Test
    void peerControlledTypeDoesNotInitializeNonThrowable() throws JsonProcessingException_v0_3 {
        assertFalse(nonThrowableInitialized);

        JSONRPCError_v0_3 error = deserializeError(NonThrowable.class.getName());

        assertFalse(nonThrowableInitialized, "The peer-selected class must not be initialized");
        assertEquals("owned", assertInstanceOf(RuntimeException.class, error.getData()).getMessage());
    }

    @Test
    void peerControlledThrowableIsNotInstantiated() throws JsonProcessingException_v0_3 {
        JSONRPCError_v0_3 error = deserializeError(CustomThrowable.class.getName());

        RuntimeException data = assertInstanceOf(RuntimeException.class, error.getData());
        assertEquals(RuntimeException.class, data.getClass());
        assertEquals("owned", data.getMessage());
    }

    private JSONRPCError_v0_3 deserializeError(String type) throws JsonProcessingException_v0_3 {
        String json = """
                {"code":-32603,"message":"boom","data":{"type":"%s","message":"owned","__throwable":true}}
                """.formatted(type);
        return JsonUtil_v0_3.fromJson(json, JSONRPCError_v0_3.class);
    }
}
