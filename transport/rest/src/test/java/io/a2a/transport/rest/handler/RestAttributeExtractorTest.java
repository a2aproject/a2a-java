package io.a2a.transport.rest.handler;

import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_CONFIG_ID;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_CONTEXT_ID;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_EXTENSIONS;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_PROTOCOL;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_REQUEST;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_STATUS;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_TASK_ID;
import static io.a2a.transport.rest.context.RestContextKeys.METHOD_NAME_KEY;
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

class RestAttributeExtractorTest {

    private RestAttributeExtractor extractor;

    @BeforeEach
    void setUp() {
        System.setProperty("io.a2a.server.extract.request", "true");
        System.setProperty("io.a2a.server.extract.response", "true");
        extractor = new RestAttributeExtractor();
    }

    @AfterEach
    void tearDown() {
        System.setProperty("io.a2a.server.extract.request", "false");
        System.setProperty("io.a2a.server.extract.response", "false");
    }

    @Test
    void testExtractAttributes_SendMessage_Success() throws Exception {
        Method method = TestService.class.getMethod("sendMessage", ServerCallContext.class, String.class, String.class);

        ServerCallContext context = createContext("POST /messages", "ext1,ext2");
        Object[] parameters = new Object[]{context, "extra-param", "test-request"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("test-request", attributes.get(GENAI_REQUEST));
        assertExtensionsContain(attributes.get(GENAI_EXTENSIONS), "ext1", "ext2");
        assertEquals("POST /messages", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_SendStreamingMessage_Success() throws Exception {
        Method method = TestService.class.getMethod("sendStreamingMessage", ServerCallContext.class, String.class, String.class);

        ServerCallContext context = createContext("POST /messages/stream", "ext1");
        Object[] parameters = new Object[]{context, "extra-param", "streaming-request"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("streaming-request", attributes.get(GENAI_REQUEST));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertEquals("POST /messages/stream", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_SetTaskPushNotificationConfiguration_Success() throws Exception {
        Method method = TestService.class.getMethod("setTaskPushNotificationConfiguration",
            ServerCallContext.class, String.class, String.class, String.class);

        ServerCallContext context = createContext("PUT /tasks/task123/notifications", "ext1");
        Object[] parameters = new Object[]{context, "extra-param", "config-request", "task123"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("task123", attributes.get(GENAI_TASK_ID));
        assertEquals("config-request", attributes.get(GENAI_REQUEST));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertEquals("PUT /tasks/task123/notifications", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_CancelTask_Success() throws Exception {
        Method method = TestService.class.getMethod("cancelTask", ServerCallContext.class, String.class, String.class);

        ServerCallContext context = createContext("DELETE /tasks/task456", "ext1,ext2");
        Object[] parameters = new Object[]{context, "extra-param", "task456"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("task456", attributes.get(GENAI_TASK_ID));
        assertExtensionsContain(attributes.get(GENAI_EXTENSIONS), "ext1", "ext2");
        assertEquals("DELETE /tasks/task456", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
        assertFalse(attributes.containsKey(GENAI_REQUEST));
    }

    @Test
    void testExtractAttributes_SubscribeToTask_Success() throws Exception {
        Method method = TestService.class.getMethod("subscribeToTask", ServerCallContext.class, String.class, String.class);

        ServerCallContext context = createContext("POST /tasks/task789/subscribe", "");
        Object[] parameters = new Object[]{context, "extra-param", "task789"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("task789", attributes.get(GENAI_TASK_ID));
        assertEquals("", attributes.get(GENAI_EXTENSIONS));
        assertEquals("POST /tasks/task789/subscribe", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_GetTask_Success() throws Exception {
        Method method = TestService.class.getMethod("getTask", ServerCallContext.class, String.class, String.class, int.class);

        ServerCallContext context = createContext("GET /tasks/task101", "ext1");
        Object[] parameters = new Object[]{context, "extra-param", "task101", 10};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("task101", attributes.get(GENAI_TASK_ID));
        assertEquals("10", attributes.get("gen_ai.agent.a2a.historyLength"));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertEquals("GET /tasks/task101", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_GetTaskPushNotificationConfiguration_Success() throws Exception {
        Method method = TestService.class.getMethod("getTaskPushNotificationConfiguration",
            ServerCallContext.class, String.class, String.class, String.class);

        ServerCallContext context = createContext("GET /tasks/task202/notifications/config1", "ext1,ext2");
        Object[] parameters = new Object[]{context, "extra-param", "task202", "config1"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("task202", attributes.get(GENAI_TASK_ID));
        assertEquals("config1", attributes.get(GENAI_CONFIG_ID));
        assertExtensionsContain(attributes.get(GENAI_EXTENSIONS), "ext1", "ext2");
        assertEquals("GET /tasks/task202/notifications/config1", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_DeleteTaskPushNotificationConfiguration_Success() throws Exception {
        Method method = TestService.class.getMethod("deleteTaskPushNotificationConfiguration",
            ServerCallContext.class, String.class, String.class, String.class);

        ServerCallContext context = createContext("DELETE /tasks/task303/notifications/config2", "");
        Object[] parameters = new Object[]{context, "extra-param", "task303", "config2"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("task303", attributes.get(GENAI_TASK_ID));
        assertEquals("config2", attributes.get(GENAI_CONFIG_ID));
        assertEquals("", attributes.get(GENAI_EXTENSIONS));
        assertEquals("DELETE /tasks/task303/notifications/config2", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_ListTaskPushNotificationConfigurations_Success() throws Exception {
        Method method = TestService.class.getMethod("listTaskPushNotificationConfigurations",
            ServerCallContext.class, String.class, String.class, int.class, String.class);

        ServerCallContext context = createContext("GET /tasks/task404/notifications", "ext1");
        Object[] parameters = new Object[]{context, "extra-param", "task404", 0, ""};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("task404", attributes.get(GENAI_TASK_ID));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertEquals("GET /tasks/task404/notifications", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_ListTasks_Success() throws Exception {
        Method method = TestService.class.getMethod("listTasks",
            ServerCallContext.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class);

        ServerCallContext context = createContext("GET /tasks", "ext1,ext2,ext3");
        Object[] parameters = new Object[]{context, "p1", "context1", "active", "p2", "p3", "p4", "p5", "p6"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("context1", attributes.get(GENAI_CONTEXT_ID));
        assertEquals("active", attributes.get(GENAI_STATUS));
        assertExtensionsContain(attributes.get(GENAI_EXTENSIONS), "ext1", "ext2", "ext3");
        assertEquals("GET /tasks", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_NullContext() {
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        assertThrows(NullPointerException.class, () -> function.apply(null));
    }

    @Test
    void testExtractAttributes_NullParameters() throws Exception {
        Method method = TestService.class.getMethod("sendMessage", ServerCallContext.class, String.class, String.class);
        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, null);

        Function<InvocationContext, Map<String, String>> function = extractor.get();
        assertThrows(IllegalArgumentException.class, () -> function.apply(invocationCtx));
    }

    @Test
    void testExtractAttributes_EmptyParameters() throws Exception {
        Method method = TestService.class.getMethod("sendMessage", ServerCallContext.class, String.class, String.class);
        Object[] parameters = new Object[]{};
        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);

        Function<InvocationContext, Map<String, String>> function = extractor.get();
        assertThrows(IllegalArgumentException.class, () -> function.apply(invocationCtx));
    }

    @Test
    void testExtractAttributes_UnknownMethod() throws Exception {
        Method method = TestService.class.getMethod("unknownMethod", ServerCallContext.class, String.class, String.class);
        ServerCallContext context = createContext("UNKNOWN", "ext1");
        Object[] parameters = new Object[]{context, "param1", "param2"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testExtractAttributes_NullTaskId() throws Exception {
        Method method = TestService.class.getMethod("cancelTask", ServerCallContext.class, String.class, String.class);

        ServerCallContext context = createContext("DELETE /tasks/null", "ext1");
        Object[] parameters = new Object[]{context, "extra-param", null};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertFalse(attributes.containsKey(GENAI_TASK_ID));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertEquals("DELETE /tasks/null", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_NoExtensions() throws Exception {
        Method method = TestService.class.getMethod("sendMessage", ServerCallContext.class, String.class, String.class);

        ServerCallContext context = createContext("POST /messages", null);
        Object[] parameters = new Object[]{context, "extra-param", "test-request"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("test-request", attributes.get(GENAI_REQUEST));
        assertEquals("", attributes.get(GENAI_EXTENSIONS));
        assertEquals("POST /messages", attributes.get(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
    }

    @Test
    void testExtractAttributes_NoOperationName() throws Exception {
        Method method = TestService.class.getMethod("sendMessage", ServerCallContext.class, String.class, String.class);

        ServerCallContext context = createContext(null, "ext1");
        Object[] parameters = new Object[]{context, "extra-param", "test-request"};

        InvocationContext invocationCtx = new InvocationContext(new TestService(), method, parameters);
        Function<InvocationContext, Map<String, String>> function = extractor.get();
        Map<String, String> attributes = function.apply(invocationCtx);

        assertNotNull(attributes);
        assertEquals("test-request", attributes.get(GENAI_REQUEST));
        assertEquals("ext1", attributes.get(GENAI_EXTENSIONS));
        assertFalse(attributes.containsKey(GENAI_OPERATION_NAME));
        assertEquals("HTTP+JSON", attributes.get(GENAI_PROTOCOL));
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
        public void sendMessage(ServerCallContext context, String tenant, String request) {}
        public void sendStreamingMessage(ServerCallContext context, String tenant, String request) {}
        public void setTaskPushNotificationConfiguration(ServerCallContext context, String tenant, String request, String taskId) {}
        public void cancelTask(ServerCallContext context, String tenant, String taskId) {}
        public void subscribeToTask(ServerCallContext context, String tenant, String taskId) {}
        public void listTaskPushNotificationConfigurations(ServerCallContext context, String tenant, String taskId, int pageSize, String pageToken) {}
        public void getTask(ServerCallContext context, String tenant, String taskId, int historyLength) {}
        public void getTaskPushNotificationConfiguration(ServerCallContext context, String tenant, String taskId, String configId) {}
        public void deleteTaskPushNotificationConfiguration(ServerCallContext context, String tenant, String taskId, String configId) {}
        public void listTasks(ServerCallContext context, String tenant, String contextId, String status, String p1, String p2, String p3, String p4, String p5) {}
        public void unknownMethod(ServerCallContext context, String param1, String param2) {}
    }
}
