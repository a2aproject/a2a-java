package org.a2aproject.sdk.server.requesthandlers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.auth.SimpleTaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.server.events.EventQueueUtil;
import org.a2aproject.sdk.server.events.InMemoryQueueManager;
import org.a2aproject.sdk.server.events.MainEventBus;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.server.tasks.InMemoryPushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendConfiguration;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultRequestHandlerBuilderAuthorizationTest {

    private static final MessageSendConfiguration DEFAULT_CONFIG = MessageSendConfiguration.builder()
            .returnImmediately(true)
            .acceptedOutputModes(List.of())
            .build();

    private static final PushNotificationSender NOOP_PUSH_SENDER = (event, snapshot) -> {};

    private InMemoryTaskStore taskStore;
    private InMemoryQueueManager queueManager;
    private MainEventBusProcessor mainEventBusProcessor;
    private ExecutorService executor;
    private SimpleTaskAuthorizationProvider authProvider;
    private RequestHandler handler;

    @BeforeEach
    void setUp() {
        authProvider = new SimpleTaskAuthorizationProvider();

        taskStore = new InMemoryTaskStore();
        MainEventBus mainEventBus = new MainEventBus();
        queueManager = new InMemoryQueueManager(taskStore, mainEventBus);
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, NOOP_PUSH_SENDER, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);

        executor = Executors.newCachedThreadPool();
        handler = DefaultRequestHandler.builder()
                .agentExecutor(completingAgentExecutor())
                .taskStore(taskStore)
                .queueManager(queueManager)
                .pushConfigStore(new InMemoryPushNotificationConfigStore())
                .mainEventBusProcessor(mainEventBusProcessor)
                .executor(executor)
                .eventConsumerExecutor(executor)
                .authorizationProvider(authProvider)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (mainEventBusProcessor != null) {
            EventQueueUtil.stop(mainEventBusProcessor);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Nested
    class NoProviderFailClosedTests {

        private RequestHandler failClosedHandler;

        @BeforeEach
        void setUp() {
            failClosedHandler = DefaultRequestHandler.builder()
                    .agentExecutor(completingAgentExecutor())
                    .taskStore(taskStore)
                    .queueManager(queueManager)
                    .pushConfigStore(new InMemoryPushNotificationConfigStore())
                    .mainEventBusProcessor(mainEventBusProcessor)
                    .executor(executor)
                    .eventConsumerExecutor(executor)
                    .build();
        }

        @Test
        void onGetTask_deniedWithoutProvider() {
            Task task = saveTask("task-1");

            assertThrows(TaskNotFoundError.class,
                    () -> failClosedHandler.onGetTask(new TaskQueryParams("task-1", null, null), contextForUser("anyone")));
        }

        @Test
        void onMessageSend_deniedWithoutProvider() {
            Message message = Message.builder().role(Message.Role.ROLE_USER)
                    .parts(new TextPart("hello")).build();
            MessageSendParams params = MessageSendParams.builder()
                    .message(message).configuration(DEFAULT_CONFIG).build();

            assertThrows(TaskNotFoundError.class,
                    () -> failClosedHandler.onMessageSend(params, contextForUser("anyone")));
        }

        @Test
        void onListTasks_deniedWithoutProvider() {
            assertThrows(TaskNotFoundError.class,
                    () -> failClosedHandler.onListTasks(new ListTasksParams(), contextForUser("anyone")));
        }
    }

    @Nested
    class NoProviderTests {

        private RequestHandler noAuthHandler;

        @BeforeEach
        void setUp() {
            noAuthHandler = DefaultRequestHandler.builder()
                    .agentExecutor(completingAgentExecutor())
                    .taskStore(taskStore)
                    .queueManager(queueManager)
                    .pushConfigStore(new InMemoryPushNotificationConfigStore())
                    .mainEventBusProcessor(mainEventBusProcessor)
                    .executor(executor)
                    .eventConsumerExecutor(executor)
                    .authorizationRequired(false)
                    .build();
        }

        @Test
        void onGetTask_succeedsWithoutProvider() throws A2AError {
            Task task = saveTask("task-1");

            Task result = noAuthHandler.onGetTask(new TaskQueryParams("task-1", null, null), contextForUser("anyone"));

            assertNotNull(result);
        }

        @Test
        void onMessageSend_newTask_succeedsWithoutProvider() throws A2AError {
            Message message = Message.builder().role(Message.Role.ROLE_USER)
                    .parts(new TextPart("hello")).build();
            MessageSendParams params = MessageSendParams.builder()
                    .message(message).configuration(DEFAULT_CONFIG).build();

            EventKind result = noAuthHandler.onMessageSend(params, contextForUser("anyone"));

            assertNotNull(result);
        }

        @Test
        void onListTasks_succeedsWithoutProvider() throws A2AError {
            assertDoesNotThrow(() -> noAuthHandler.onListTasks(new ListTasksParams(), contextForUser("anyone")));
        }
    }

    @Nested
    class GetTaskTests {

        @Test
        void ownerCanGetOwnTask() throws A2AError {
            Task task = saveTaskOwnedBy("userA");

            Task result = handler.onGetTask(new TaskQueryParams(task.id(), null, null), contextForUser("userA"));

            assertNotNull(result);
        }

        @Test
        void nonOwnerCannotGetTask() {
            Task task = saveTaskOwnedBy("userA");

            assertThrows(TaskNotFoundError.class,
                    () -> handler.onGetTask(new TaskQueryParams(task.id(), null, null), contextForUser("userB")));
        }
    }

    @Nested
    class CancelTaskTests {

        @Test
        void nonOwnerCannotCancelTask() {
            Task task = saveActiveTaskOwnedBy("userA");

            assertThrows(TaskNotFoundError.class,
                    () -> handler.onCancelTask(new CancelTaskParams(task.id()), contextForUser("userB")));
        }
    }

    @Nested
    class SubscribeToTaskTests {

        @Test
        void nonOwnerCannotSubscribeToTask() {
            Task task = saveActiveTaskOwnedBy("userA");

            assertThrows(TaskNotFoundError.class,
                    () -> handler.onSubscribeToTask(new TaskIdParams(task.id()), contextForUser("userB")));
        }
    }

    @Nested
    class MessageSendTests {

        @Test
        void existingTask_nonOwnerDenied() {
            Task task = saveActiveTaskOwnedBy("userA");

            Message message = Message.builder().role(Message.Role.ROLE_USER)
                    .taskId(task.id()).parts(new TextPart("hello")).build();
            MessageSendParams params = MessageSendParams.builder()
                    .message(message).configuration(DEFAULT_CONFIG).build();

            assertThrows(TaskNotFoundError.class,
                    () -> handler.onMessageSend(params, contextForUser("userB")));
        }

        @Test
        void newTask_recordsOwnership() throws A2AError {
            Message message = Message.builder().role(Message.Role.ROLE_USER)
                    .parts(new TextPart("hello")).build();
            MessageSendParams params = MessageSendParams.builder()
                    .message(message).configuration(DEFAULT_CONFIG).build();

            ServerCallContext contextA = contextForUser("userA");
            EventKind result = handler.onMessageSend(params, contextA);

            assertNotNull(result);

            // The created task should be owned by userA — userB should not be able to read it
            Task createdTask = (Task) result;
            assertThrows(TaskNotFoundError.class,
                    () -> handler.onGetTask(new TaskQueryParams(createdTask.id(), null, null), contextForUser("userB")));
        }
    }

    @Nested
    class PushNotificationTests {

        @Test
        void createPushConfig_nonOwnerDenied() {
            Task task = saveTaskOwnedBy("userA");
            TaskPushNotificationConfig config = TaskPushNotificationConfig.builder()
                    .id("config-1").taskId(task.id()).url("https://example.com/webhook").build();

            assertThrows(TaskNotFoundError.class,
                    () -> handler.onCreateTaskPushNotificationConfig(config, contextForUser("userB")));
        }

        @Test
        void getPushConfig_nonOwnerDenied() {
            Task task = saveTaskOwnedBy("userA");
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams(task.id(), "config-1");

            assertThrows(TaskNotFoundError.class,
                    () -> handler.onGetTaskPushNotificationConfig(params, contextForUser("userB")));
        }

        @Test
        void listPushConfigs_nonOwnerDenied() {
            Task task = saveTaskOwnedBy("userA");
            ListTaskPushNotificationConfigsParams params = new ListTaskPushNotificationConfigsParams(task.id());

            assertThrows(TaskNotFoundError.class,
                    () -> handler.onListTaskPushNotificationConfigs(params, contextForUser("userB")));
        }

        @Test
        void deletePushConfig_nonOwnerDenied() {
            Task task = saveTaskOwnedBy("userA");
            DeleteTaskPushNotificationConfigParams params =
                    new DeleteTaskPushNotificationConfigParams(task.id(), "config-1");

            assertThrows(TaskNotFoundError.class,
                    () -> handler.onDeleteTaskPushNotificationConfig(params, contextForUser("userB")));
        }
    }

    @Nested
    class ListTasksTests {

        @Test
        void listTasks_denied() throws A2AError {
            SimpleTaskAuthorizationProvider denyListProvider = new SimpleTaskAuthorizationProvider() {
                @Override
                public boolean checkRead(ServerCallContext context, String taskId, TaskOperation operation) {
                    if (operation == TaskOperation.LIST_TASKS && taskId.isEmpty()) {
                        return false;
                    }
                    return super.checkRead(context, taskId, operation);
                }
            };

            RequestHandler restrictedHandler = DefaultRequestHandler.builder()
                    .agentExecutor(completingAgentExecutor())
                    .taskStore(taskStore)
                    .queueManager(queueManager)
                    .pushConfigStore(new InMemoryPushNotificationConfigStore())
                    .mainEventBusProcessor(mainEventBusProcessor)
                    .executor(executor)
                    .eventConsumerExecutor(executor)
                    .authorizationProvider(denyListProvider)
                    .build();

            assertThrows(TaskNotFoundError.class,
                    () -> restrictedHandler.onListTasks(new ListTasksParams(), contextForUser("anyone")));
        }

        @Test
        void listTasks_perTaskFiltering_nonOwnerSeesNothing() throws A2AError {
            saveTaskOwnedBy("userA");

            ListTasksResult result = handler.onListTasks(new ListTasksParams(), contextForUser("userB"));

            assertEquals(0, result.tasks().size());
        }

        @Test
        void listTasks_perTaskFiltering_ownerSeesOwnTasks() throws A2AError {
            saveTaskOwnedBy("userA");
            saveTaskOwnedBy("userB");

            ListTasksResult resultA = handler.onListTasks(new ListTasksParams(), contextForUser("userA"));
            ListTasksResult resultB = handler.onListTasks(new ListTasksParams(), contextForUser("userB"));

            assertEquals(1, resultA.tasks().size());
            assertEquals(1, resultB.tasks().size());
        }
    }

    private Task saveTask(String taskId) {
        Task task = Task.builder()
                .id(taskId).contextId("ctx-" + taskId)
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();
        taskStore.save(task, false);
        return task;
    }

    private Task saveTaskOwnedBy(String owner) {
        String taskId = "task-" + owner + "-" + System.nanoTime();
        Task task = Task.builder()
                .id(taskId).contextId("ctx-" + taskId)
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();
        taskStore.save(task, false);
        authProvider.recordOwnership(contextForUser(owner), taskId, TaskOperation.MESSAGE_SEND);
        return task;
    }

    private Task saveActiveTaskOwnedBy(String owner) {
        String taskId = "task-" + owner + "-" + System.nanoTime();
        Task task = Task.builder()
                .id(taskId).contextId("ctx-" + taskId)
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();
        taskStore.save(task, false);
        authProvider.recordOwnership(contextForUser(owner), taskId, TaskOperation.MESSAGE_SEND);
        return task;
    }

    private static ServerCallContext contextForUser(String username) {
        return new ServerCallContext(new TestUser(username), Map.of(), Set.of());
    }

    private static AgentExecutor completingAgentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                agentEmitter.complete();
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                agentEmitter.cancel();
            }
        };
    }

    private record TestUser(String username) implements User {
        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public String getUsername() {
            return username;
        }
    }
}
