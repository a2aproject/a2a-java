package io.a2a.transport.grpc.handler;

import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_EXTENSIONS;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_PROTOCOL;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_REQUEST;

import io.a2a.server.interceptors.InvocationContext;
import io.a2a.transport.grpc.context.GrpcContextKeys;
import io.grpc.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;



class GrpcAttributeExtractorTest {

    private GrpcAttributeExtractor extractor;
    private Context previousContext;

    @BeforeEach
    void setUp() {
        System.setProperty("io.a2a.server.extract.request", "true");
        System.setProperty("io.a2a.server.extract.response", "true");
        extractor = new GrpcAttributeExtractor();
        // Save the current context to restore it later
        previousContext = Context.current();
    }

    @AfterEach
    void tearDown() {
        // Restore the previous context
        if (previousContext != null) {
            previousContext.attach();
        }
        System.setProperty("io.a2a.server.extract.request", "false");
        System.setProperty("io.a2a.server.extract.response", "false");
    }

    @Test
    void testExtractAttributes_SendMessage_Success() throws Exception {
        // Create a mock method
        Method method = TestService.class.getMethod("sendMessage", Object.class);
        Object[] parameters = new Object[]{"test-request"};

        // Set up gRPC context with test values
        Context ctx = Context.current()
                .withValue(GrpcContextKeys.GRPC_METHOD_NAME_KEY, "a2a.SendMessage")
                .withValue(GrpcContextKeys.EXTENSIONS_HEADER_KEY, "ext1,ext2");

        Context previous = ctx.attach();
        try {
            InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
            Function<InvocationContext, Map<String, String>> function = extractor.get();
            Map<String, String> attributes = function.apply(invocationCtx);

            assertNotNull(attributes);
            assertEquals(4, attributes.size());
            assertEquals("test-request", attributes.get(GENAI_REQUEST));
            assertEquals("ext1,ext2", attributes.get(GENAI_EXTENSIONS));
            assertEquals("a2a.SendMessage", attributes.get(GENAI_OPERATION_NAME));
            assertEquals("GRPC", attributes.get(GENAI_PROTOCOL));
        } finally {
            ctx.detach(previous);
        }
    }

    @Test
    void testExtractAttributes_NullContext() {
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(null);

        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testExtractAttributes_NullMethod() throws Exception {
        Object[] parameters = new Object[]{"test-request"};
        assertThrows(IllegalArgumentException.class, () -> new InvocationContext(new TestService(), null, parameters));
    }

    @Test
    void testExtractAttributes_NullParameters() throws Exception {
        Method method = TestService.class.getMethod("sendMessage", Object.class);
        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, null);

        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testExtractAttributes_EmptyParameters() throws Exception {
        Method method = TestService.class.getMethod("sendMessage", Object.class);
        Object[] parameters = new Object[]{};
        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);

        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testExtractAttributes_NullParameter() throws Exception {
        Method method = TestService.class.getMethod("sendMessage", Object.class);
        Object[] parameters = new Object[]{null};

        Context ctx = Context.current()
                .withValue(GrpcContextKeys.GRPC_METHOD_NAME_KEY, "a2a.SendMessage");

        Context previous = ctx.attach();
        try {
            InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
            Function<InvocationContext, Map<String, String>> function = extractor.get();
            Map<String, String> attributes = function.apply(invocationCtx);

            assertNotNull(attributes);
            // Should have operation name but not request (since parameter is null)
            assertEquals(2, attributes.size());
            assertEquals("a2a.SendMessage", attributes.get(GENAI_OPERATION_NAME));
            assertEquals("GRPC", attributes.get(GENAI_PROTOCOL));
        } finally {
            ctx.detach(previous);
        }
    }

    @Test
    void testExtractAttributes_NoGrpcContextKeys() throws Exception {
        Method method = TestService.class.getMethod("sendMessage", Object.class);
        Object[] parameters = new Object[]{"test-request"};

        // Don't set any context values
        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(2, attributes.size());
        assertEquals("test-request", attributes.get(GENAI_REQUEST));
        assertFalse(attributes.containsKey(GENAI_EXTENSIONS));
        assertFalse(attributes.containsKey(GENAI_OPERATION_NAME));
        assertEquals("GRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_UnknownMethod() throws Exception {
        Method method = TestService.class.getMethod("unknownMethod");
        Object[] parameters = new Object[]{};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testExtractAttributes_AllSupportedMethods() throws Exception {
        String[] supportedMethods = {
            "sendMessage",
            "getTask",
            "listTasks",
            "cancelTask",
            "createTaskPushNotificationConfig",
            "getTaskPushNotificationConfig",
            "listTaskPushNotificationConfig",
            "sendStreamingMessage",
            "subscribeToTask",
            "deleteTaskPushNotificationConfig"
        };

        for (String methodName : supportedMethods) {
            Method method = TestService.class.getMethod(methodName, Object.class);
            Object[] parameters = new Object[]{"request-" + methodName};

            Context ctx = Context.current()
                    .withValue(GrpcContextKeys.GRPC_METHOD_NAME_KEY, "a2a." + methodName)
                    .withValue(GrpcContextKeys.EXTENSIONS_HEADER_KEY, "ext1");

            Context previous = ctx.attach();
            try {
                InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
                Function<InvocationContext, Map<String, String>> function = extractor.get();
                Map<String, String> attributes = function.apply(invocationCtx);

                assertNotNull(attributes, "Attributes should not be null for method: " + methodName);
                assertFalse(attributes.isEmpty(), "Attributes should not be empty for method: " + methodName);
                assertEquals("request-" + methodName, attributes.get(GENAI_REQUEST),
                        "Request attribute should match for method: " + methodName);
                assertEquals("ext1", attributes.get(GENAI_EXTENSIONS),
                        "Extensions should match for method: " + methodName);
                assertEquals("a2a." + methodName, attributes.get(GENAI_OPERATION_NAME),
                        "Operation name should match for method: " + methodName);
            } finally {
                ctx.detach(previous);
            }
        }
    }

    @Test
    void testExtractAttributes_OnlyExtensions() throws Exception {
        Method method = TestService.class.getMethod("sendMessage", Object.class);
        Object[] parameters = new Object[]{null};

        Context ctx = Context.current()
                .withValue(GrpcContextKeys.EXTENSIONS_HEADER_KEY, "ext1,ext2,ext3");

        Context previous = ctx.attach();
        try {
            InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
            Function<InvocationContext, Map<String, String>> function = extractor.get();
            Map<String, String> attributes = function.apply(invocationCtx);

            assertNotNull(attributes);
            assertEquals(2, attributes.size());
            assertEquals("ext1,ext2,ext3", attributes.get(GENAI_EXTENSIONS));
            assertEquals("GRPC", attributes.get(GENAI_PROTOCOL));
        } finally {
            ctx.detach(previous);
        }
    }

    // Test service class with all supported methods
    public static class TestService {
        public void sendMessage(Object request) {}
        public void getTask(Object request) {}
        public void listTasks(Object request) {}
        public void cancelTask(Object request) {}
        public void createTaskPushNotificationConfig(Object request) {}
        public void getTaskPushNotificationConfig(Object request) {}
        public void listTaskPushNotificationConfig(Object request) {}
        public void sendStreamingMessage(Object request) {}
        public void subscribeToTask(Object request) {}
        public void deleteTaskPushNotificationConfig(Object request) {}
        public void unknownMethod() {}
    }
}
