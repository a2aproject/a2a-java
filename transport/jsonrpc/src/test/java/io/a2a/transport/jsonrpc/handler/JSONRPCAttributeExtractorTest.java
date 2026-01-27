package io.a2a.transport.jsonrpc.handler;

import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_EXTENSIONS;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_PROTOCOL;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_REQUEST;
import static io.a2a.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;
import static org.junit.jupiter.api.Assertions.*;

import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.User;
import io.a2a.server.interceptors.InvocationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

class JSONRPCAttributeExtractorTest {

    private JSONRPCAttributeExtractor extractor;

    @BeforeEach
    void setUp() {
        System.setProperty("io.a2a.server.extract.request", "true");
        System.setProperty("io.a2a.server.extract.response", "true");
        extractor = new JSONRPCAttributeExtractor();
    }

    @AfterEach
    void tearDown() {
        System.setProperty("io.a2a.server.extract.request", "false");
        System.setProperty("io.a2a.server.extract.response", "false");
    }

    @Test
    void testExtractAttributes_OnMessageSend_Success() throws Exception {
        Method method = TestService.class.getMethod("onMessageSend", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("messages/send", "ext1,ext2");
        Object[] parameters = new Object[]{"test-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("test-request", attributes.get(GENAI_REQUEST));
        assertExtensionsContain(attributes.get(GENAI_EXTENSIONS), "ext1", "ext2");
        assertEquals("messages/send", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_OnMessageSendStream_Success() throws Exception {
        Method method = TestService.class.getMethod("onMessageSendStream", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("messages/sendStream", "ext1");
        Object[] parameters = new Object[]{"streaming-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("streaming-request", attributes.get(GENAI_REQUEST));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertEquals("messages/sendStream", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_OnCancelTask_Success() throws Exception {
        Method method = TestService.class.getMethod("onCancelTask", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("tasks/cancel", "ext1,ext2,ext3");
        Object[] parameters = new Object[]{"cancel-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("cancel-request", attributes.get(GENAI_REQUEST));
        assertExtensionsContain(attributes.get(GENAI_EXTENSIONS), "ext1", "ext2", "ext3");
        assertEquals("tasks/cancel", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_OnResubscribeToTask_Success() throws Exception {
        Method method = TestService.class.getMethod("onResubscribeToTask", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("tasks/resubscribe", "");
        Object[] parameters = new Object[]{"resubscribe-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("resubscribe-request", attributes.get(GENAI_REQUEST));
        assertEquals("", attributes.get(GENAI_EXTENSIONS));
        assertEquals("tasks/resubscribe", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_GetPushNotificationConfig_Success() throws Exception {
        Method method = TestService.class.getMethod("getPushNotificationConfig", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("notifications/get", "ext1");
        Object[] parameters = new Object[]{"get-config-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("get-config-request", attributes.get(GENAI_REQUEST));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertEquals("notifications/get", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_SetPushNotificationConfig_Success() throws Exception {
        Method method = TestService.class.getMethod("setPushNotificationConfig", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("notifications/set", "ext1,ext2");
        Object[] parameters = new Object[]{"set-config-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("set-config-request", attributes.get(GENAI_REQUEST));
        assertExtensionsContain(attributes.get(GENAI_EXTENSIONS), "ext1", "ext2");
        assertEquals("notifications/set", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_OnGetTask_Success() throws Exception {
        Method method = TestService.class.getMethod("onGetTask", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("tasks/get", "ext1");
        Object[] parameters = new Object[]{"get-task-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("get-task-request", attributes.get(GENAI_REQUEST));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertEquals("tasks/get", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_ListPushNotificationConfig_Success() throws Exception {
        Method method = TestService.class.getMethod("listPushNotificationConfig", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("notifications/list", "ext1,ext2");
        Object[] parameters = new Object[]{"list-config-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("list-config-request", attributes.get(GENAI_REQUEST));
        assertExtensionsContain(attributes.get(GENAI_EXTENSIONS), "ext1", "ext2");
        assertEquals("notifications/list", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_DeletePushNotificationConfig_Success() throws Exception {
        Method method = TestService.class.getMethod("deletePushNotificationConfig", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("notifications/delete", "ext1");
        Object[] parameters = new Object[]{"delete-config-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("delete-config-request", attributes.get(GENAI_REQUEST));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertEquals("notifications/delete", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_OnListTasks_Success() throws Exception {
        Method method = TestService.class.getMethod("onListTasks", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("tasks/list", "ext1,ext2");
        Object[] parameters = new Object[]{"list-tasks-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("list-tasks-request", attributes.get(GENAI_REQUEST));
        assertExtensionsContain(attributes.get(GENAI_EXTENSIONS), "ext1", "ext2");
        assertEquals("tasks/list", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_NullContext() {
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        assertThrows(NullPointerException.class, () -> function.apply(null));
    }

    @Test
    void testExtractAttributes_NullParameters() throws Exception {
        Method method = TestService.class.getMethod("onMessageSend", Object.class, ServerCallContext.class);
        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, null);

        Function<InvocationContext, Map<String, String>> function = extractor.get();
        assertThrows(IllegalArgumentException.class, () -> function.apply(invocationCtx));
    }

    @Test
    void testExtractAttributes_EmptyParameters() throws Exception {
        Method method = TestService.class.getMethod("onMessageSend", Object.class, ServerCallContext.class);
        Object[] parameters = new Object[]{};
        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);

        Function<InvocationContext, Map<String, String>> function = extractor.get();
        assertThrows(IllegalArgumentException.class, () -> function.apply(invocationCtx));
    }

    @Test
    void testExtractAttributes_UnknownMethod() throws Exception {
        Method method = TestService.class.getMethod("unknownMethod", Object.class, ServerCallContext.class);
        ServerCallContext context = createContext("unknown/method", "ext1");
        Object[] parameters = new Object[]{"request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testExtractAttributes_NullRequest() throws Exception {
        Method method = TestService.class.getMethod("onMessageSend", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("messages/send", "ext1");
        Object[] parameters = new Object[]{null, context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        // Should have operation name, protocol, and extensions but not request (since parameter is null)
        assertEquals(3, attributes.size());
        assertFalse(attributes.containsKey(GENAI_REQUEST));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertEquals("messages/send", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_NoExtensions() throws Exception {
        Method method = TestService.class.getMethod("onMessageSend", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext("messages/send", null);
        Object[] parameters = new Object[]{"test-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(4, attributes.size());
        assertEquals("test-request", attributes.get(GENAI_REQUEST));
        assertEquals("", attributes.get(GENAI_EXTENSIONS));
        assertEquals("messages/send", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_NoOperationName() throws Exception {
        Method method = TestService.class.getMethod("onMessageSend", Object.class, ServerCallContext.class);

        ServerCallContext context = createContext(null, "ext1,ext2");
        Object[] parameters = new Object[]{"test-request", context};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals(3, attributes.size());
        assertEquals("test-request", attributes.get(GENAI_REQUEST));
        assertExtensionsContain(attributes.get(GENAI_EXTENSIONS), "ext1", "ext2");
        assertFalse(attributes.containsKey(GENAI_OPERATION_NAME));
        assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_AllSupportedMethods() throws Exception {
        String[] supportedMethods = {
            "onMessageSend",
            "onMessageSendStream",
            "onCancelTask",
            "onResubscribeToTask",
            "getPushNotificationConfig",
            "setPushNotificationConfig",
            "onGetTask",
            "listPushNotificationConfig",
            "deletePushNotificationConfig",
            "onListTasks"
        };

        for (String methodName : supportedMethods) {
            Method method = TestService.class.getMethod(methodName, Object.class, ServerCallContext.class);
            Object[] parameters = new Object[]{"request-" + methodName, createContext(methodName, "ext1")};

            InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
            Function<InvocationContext, Map<String, String>> function = extractor.get();
            Map<String, String> attributes = function.apply(invocationCtx);

            assertNotNull(attributes, "Attributes should not be null for method: " + methodName);
            assertFalse(attributes.isEmpty(), "Attributes should not be empty for method: " + methodName);
            assertEquals("request-" + methodName, attributes.get(GENAI_REQUEST),
                    "Request attribute should match for method: " + methodName);
            assertEquals("ext1", attributes.get(GENAI_EXTENSIONS),
                    "Extensions should match for method: " + methodName);
            assertEquals(methodName, attributes.get(GENAI_OPERATION_NAME),
                    "Operation name should match for method: " + methodName);
            assertEquals("JSONRPC", attributes.get(GENAI_PROTOCOL),
                    "Protocol should match for method: " + methodName);
        }
    }

    private ServerCallContext createContext(String methodName, String extensions) {
        Map<String, Object> state = new HashMap<>();
        if (methodName != null) {
            state.put(METHOD_NAME_KEY, methodName);
        }

        Set<String> requestedExtensions = new HashSet<>();
        if (extensions != null && !extensions.isEmpty()) {
            for (String ext : extensions.split(",")) {
                requestedExtensions.add(ext.trim());
            }
        }

        User mockUser = new User() {
            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public String getUsername() {
                return "testuser";
            }
        };

        ServerCallContext context = new ServerCallContext(mockUser, state, requestedExtensions);

        // Activate the requested extensions
        for (String ext : requestedExtensions) {
            context.activateExtension(ext);
        }

        return context;
    }

    private void assertExtensionsContain(String extensionsString, String... expectedExtensions) {
        Set<String> actualExtensions = new HashSet<>();
        if (extensionsString != null && !extensionsString.isEmpty()) {
            for (String ext : extensionsString.split(",")) {
                actualExtensions.add(ext.trim());
            }
        }

        Set<String> expectedSet = new HashSet<>();
        for (String ext : expectedExtensions) {
            expectedSet.add(ext.trim());
        }

        assertEquals(expectedSet, actualExtensions,
            "Extensions should match (order independent): expected " + expectedSet + " but got " + actualExtensions);
    }

    // Test service class with all supported methods
    public static class TestService {
        public void onMessageSend(Object request, ServerCallContext context) {}
        public void onMessageSendStream(Object request, ServerCallContext context) {}
        public void onCancelTask(Object request, ServerCallContext context) {}
        public void onResubscribeToTask(Object request, ServerCallContext context) {}
        public void getPushNotificationConfig(Object request, ServerCallContext context) {}
        public void setPushNotificationConfig(Object request, ServerCallContext context) {}
        public void onGetTask(Object request, ServerCallContext context) {}
        public void listPushNotificationConfig(Object request, ServerCallContext context) {}
        public void deletePushNotificationConfig(Object request, ServerCallContext context) {}
        public void onListTasks(Object request, ServerCallContext context) {}
        public void unknownMethod(Object request, ServerCallContext context) {}
    }
}
