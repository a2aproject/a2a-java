package org.a2aproject.sdk.server.requesthandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.auth.SimpleTaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.server.events.EventQueueUtil;
import org.a2aproject.sdk.server.events.InMemoryQueueManager;
import org.a2aproject.sdk.server.events.MainEventBus;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.events.QueueManager;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.server.tasks.InMemoryPushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.PushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.server.tasks.TaskStoreException;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.InvalidParamsError;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DefaultRequestHandlerBuilderAuthorizationTest {

    private static final MessageSendConfiguration DEFAULT_CONFIG =
            MessageSendConfiguration.builder()
                    .returnImmediately(true)
                    .acceptedOutputModes(List.of())
                    .build();

    private RequestHandler handlerWith(TaskStore store, TaskAuthorizationProvider authorization) {
        return DefaultRequestHandler.builder()
                .agentExecutor(completingAgentExecutor())
                .taskStore(store)
                .queueManager(queueManager)
                .pushConfigStore(new InMemoryPushNotificationConfigStore())
                .mainEventBusProcessor(mainEventBusProcessor)
                .executor(executor)
                .eventConsumerExecutor(executor)
                .authorizationProvider(authorization)
                .build();
    }

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
        mainEventBusProcessor =
                new MainEventBusProcessor(
                        mainEventBus, taskStore, PushNotificationSender.NOOP, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);

        executor = Executors.newCachedThreadPool();
        handler =
                DefaultRequestHandler.builder()
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

    @ParameterizedTest(name = "{0} rejects blank {1}")
    @CsvSource({
        "getTask, ''", "getTask, '   '",
        "cancelTask, ''", "cancelTask, '   '",
        "subscribe, ''", "subscribe, '   '",
        "createPush, ''", "createPush, '   '",
        "getPush, ''", "getPush, '   '",
        "listPush, ''", "listPush, '   '",
        "deletePush, ''", "deletePush, '   '",
        "deletePushId, ''", "deletePushId, '   '"
    })
    void effectiveCardRejectsBlankRequiredIdsBeforeDependencies(String operation, String id) {
        AgentExecutor agent = mock(AgentExecutor.class);
        TaskStore store = mock(TaskStore.class);
        PushNotificationConfigStore pushStore = mock(PushNotificationConfigStore.class);
        TaskAuthorizationProvider authorization = mock(TaskAuthorizationProvider.class);
        QueueManager queues = mock(QueueManager.class);
        Executor execution = mock(Executor.class);
        RequestHandler validatedHandler =
                DefaultRequestHandler.builder()
                        .agentExecutor(agent)
                        .taskStore(store)
                        .queueManager(queues)
                        .pushConfigStore(pushStore)
                        .mainEventBusProcessor(mainEventBusProcessor)
                        .executor(execution)
                        .eventConsumerExecutor(execution)
                        .authorizationProvider(authorization)
                        .build();
        ServerCallContext context = DefaultRequestHandlerTest.contextWithEffectiveCard(true);

        assertThrows(
                InvalidParamsError.class,
                () -> {
                    switch (operation) {
                        case "getTask" ->
                                validatedHandler.onGetTask(
                                        new TaskQueryParams(id, null, null), context);
                        case "cancelTask" ->
                                validatedHandler.onCancelTask(new CancelTaskParams(id), context);
                        case "subscribe" ->
                                validatedHandler.onSubscribeToTask(new TaskIdParams(id), context);
                        case "createPush" ->
                                validatedHandler.onCreateTaskPushNotificationConfig(
                                        TaskPushNotificationConfig.builder()
                                                .taskId(id)
                                                .url("https://example.com/push")
                                                .build(),
                                        context);
                        case "getPush" ->
                                validatedHandler.onGetTaskPushNotificationConfig(
                                        new GetTaskPushNotificationConfigParams(id, "config"),
                                        context);
                        case "getPushId" ->
                                validatedHandler.onGetTaskPushNotificationConfig(
                                        new GetTaskPushNotificationConfigParams("task", id),
                                        context);
                        case "listPush" ->
                                validatedHandler.onListTaskPushNotificationConfigs(
                                        new ListTaskPushNotificationConfigsParams(id), context);
                        case "deletePush" ->
                                validatedHandler.onDeleteTaskPushNotificationConfig(
                                        new DeleteTaskPushNotificationConfigParams(id, "config"),
                                        context);
                        case "deletePushId" ->
                                validatedHandler.onDeleteTaskPushNotificationConfig(
                                        new DeleteTaskPushNotificationConfigParams("task", id),
                                        context);
                        default -> throw new AssertionError(operation);
                    }
                });
        verifyNoInteractions(agent, store, pushStore, authorization, queues, execution);
    }

    @Test
    void blankTaskIdWithoutEffectiveCardRetainsAuthorizationBehavior() {
        assertThrows(
                TaskNotFoundError.class,
                () ->
                        handler.onGetTask(
                                new TaskQueryParams("", null, null), contextForUser("anyone")));
    }

    @Nested
    class NoProviderFailClosedTests {

        private RequestHandler failClosedHandler;

        @BeforeEach
        void setUp() {
            failClosedHandler =
                    DefaultRequestHandler.builder()
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

            assertThrows(
                    TaskNotFoundError.class,
                    () ->
                            failClosedHandler.onGetTask(
                                    new TaskQueryParams("task-1", null, null),
                                    contextForUser("anyone")));
        }

        @Test
        void onMessageSend_deniedWithoutProvider() {
            Message message =
                    Message.builder()
                            .role(Message.Role.ROLE_USER)
                            .parts(new TextPart("hello"))
                            .build();
            MessageSendParams params =
                    MessageSendParams.builder()
                            .message(message)
                            .configuration(DEFAULT_CONFIG)
                            .build();

            assertThrows(
                    TaskNotFoundError.class,
                    () -> failClosedHandler.onMessageSend(params, contextForUser("anyone")));
        }

        @Test
        void onListTasks_deniedWithoutProviderBeforeQueryingStore() {
            TaskStore untrustedStore = mock(TaskStore.class);
            RequestHandler failClosedBeforeStore =
                    DefaultRequestHandler.builder()
                            .agentExecutor(completingAgentExecutor())
                            .taskStore(untrustedStore)
                            .queueManager(queueManager)
                            .pushConfigStore(new InMemoryPushNotificationConfigStore())
                            .mainEventBusProcessor(mainEventBusProcessor)
                            .executor(executor)
                            .eventConsumerExecutor(executor)
                            .build();

            assertThrows(
                    TaskNotFoundError.class,
                    () ->
                            failClosedBeforeStore.onListTasks(
                                    new ListTasksParams(), contextForUser("anyone")));
            verifyNoInteractions(untrustedStore);
        }
    }

    @Nested
    class NoProviderTests {

        private RequestHandler noAuthHandler;

        @BeforeEach
        void setUp() {
            noAuthHandler =
                    DefaultRequestHandler.builder()
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

            Task result =
                    noAuthHandler.onGetTask(
                            new TaskQueryParams("task-1", null, null), contextForUser("anyone"));

            assertNotNull(result);
        }

        @Test
        void onMessageSend_newTask_succeedsWithoutProvider() throws A2AError {
            Message message =
                    Message.builder()
                            .role(Message.Role.ROLE_USER)
                            .parts(new TextPart("hello"))
                            .build();
            MessageSendParams params =
                    MessageSendParams.builder()
                            .message(message)
                            .configuration(DEFAULT_CONFIG)
                            .build();

            EventKind result = noAuthHandler.onMessageSend(params, contextForUser("anyone"));

            assertNotNull(result);
        }

        @Test
        void onListTasks_succeedsWithoutProvider() throws A2AError {
            Task task = saveTask("task-1");

            ListTasksResult result =
                    noAuthHandler.onListTasks(new ListTasksParams(), contextForUser("anyone"));

            assertEquals(List.of(task), result.tasks());
        }
    }

    @Nested
    class GetTaskTests {

        @Test
        void ownerCanGetOwnTask() throws A2AError {
            Task task = saveTaskOwnedBy("userA");

            Task result =
                    handler.onGetTask(
                            new TaskQueryParams(task.id(), null, null), contextForUser("userA"));

            assertNotNull(result);
        }

        @Test
        void nonOwnerCannotGetTask() {
            Task task = saveTaskOwnedBy("userA");

            assertThrows(
                    TaskNotFoundError.class,
                    () ->
                            handler.onGetTask(
                                    new TaskQueryParams(task.id(), null, null),
                                    contextForUser("userB")));
        }
    }

    @Nested
    class CancelTaskTests {

        @Test
        void nonOwnerCannotCancelTask() {
            Task task = saveActiveTaskOwnedBy("userA");

            assertThrows(
                    TaskNotFoundError.class,
                    () ->
                            handler.onCancelTask(
                                    new CancelTaskParams(task.id()), contextForUser("userB")));
        }
    }

    @Nested
    class SubscribeToTaskTests {

        @Test
        void nonOwnerCannotSubscribeToTask() {
            Task task = saveActiveTaskOwnedBy("userA");

            assertThrows(
                    TaskNotFoundError.class,
                    () ->
                            handler.onSubscribeToTask(
                                    new TaskIdParams(task.id()), contextForUser("userB")));
        }
    }

    @Nested
    class MessageSendTests {

        @Test
        void existingTask_nonOwnerDenied() {
            Task task = saveActiveTaskOwnedBy("userA");

            Message message =
                    Message.builder()
                            .role(Message.Role.ROLE_USER)
                            .taskId(task.id())
                            .parts(new TextPart("hello"))
                            .build();
            MessageSendParams params =
                    MessageSendParams.builder()
                            .message(message)
                            .configuration(DEFAULT_CONFIG)
                            .build();

            assertThrows(
                    TaskNotFoundError.class,
                    () -> handler.onMessageSend(params, contextForUser("userB")));
        }

        @Test
        void newTask_recordsOwnership() throws A2AError {
            Message message =
                    Message.builder()
                            .role(Message.Role.ROLE_USER)
                            .parts(new TextPart("hello"))
                            .build();
            MessageSendParams params =
                    MessageSendParams.builder()
                            .message(message)
                            .configuration(DEFAULT_CONFIG)
                            .build();

            ServerCallContext contextA = contextForUser("userA");
            EventKind result = handler.onMessageSend(params, contextA);

            assertNotNull(result);

            // The created task should be owned by userA — userB should not be able to read it
            Task createdTask = (Task) result;
            assertThrows(
                    TaskNotFoundError.class,
                    () ->
                            handler.onGetTask(
                                    new TaskQueryParams(createdTask.id(), null, null),
                                    contextForUser("userB")));
        }
    }

    @Nested
    class PushNotificationTests {

        @Test
        void createPushConfig_nonOwnerDenied() {
            Task task = saveTaskOwnedBy("userA");
            TaskPushNotificationConfig config =
                    TaskPushNotificationConfig.builder()
                            .id("config-1")
                            .taskId(task.id())
                            .url("https://example.com/webhook")
                            .build();

            assertThrows(
                    TaskNotFoundError.class,
                    () ->
                            handler.onCreateTaskPushNotificationConfig(
                                    config, contextForUser("userB")));
        }

        @Test
        void getPushConfig_nonOwnerDenied() {
            Task task = saveTaskOwnedBy("userA");
            GetTaskPushNotificationConfigParams params =
                    new GetTaskPushNotificationConfigParams(task.id(), "config-1");

            assertThrows(
                    TaskNotFoundError.class,
                    () -> handler.onGetTaskPushNotificationConfig(params, contextForUser("userB")));
        }

        @Test
        void listPushConfigs_nonOwnerDenied() {
            Task task = saveTaskOwnedBy("userA");
            ListTaskPushNotificationConfigsParams params =
                    new ListTaskPushNotificationConfigsParams(task.id());

            assertThrows(
                    TaskNotFoundError.class,
                    () ->
                            handler.onListTaskPushNotificationConfigs(
                                    params, contextForUser("userB")));
        }

        @Test
        void deletePushConfig_nonOwnerDenied() {
            Task task = saveTaskOwnedBy("userA");
            DeleteTaskPushNotificationConfigParams params =
                    new DeleteTaskPushNotificationConfigParams(task.id(), "config-1");

            assertThrows(
                    TaskNotFoundError.class,
                    () ->
                            handler.onDeleteTaskPushNotificationConfig(
                                    params, contextForUser("userB")));
        }
    }

    @Nested
    class ListTasksTests {

        @Test
        void listTasks_usesStoreAuthorizationBeforePagination() throws A2AError {
            TaskStore awareStore = mock(TaskStore.class);
            TaskAuthorizationProvider authorization = mock(TaskAuthorizationProvider.class);
            ServerCallContext context = contextForUser("anyone");
            ListTasksParams params = new ListTasksParams(1, null);
            ListTasksResult storedResult = new ListTasksResult(List.of(), 0, 0, null);
            when(authorization.checkRead(context, "", TaskOperation.LIST_TASKS)).thenReturn(true);
            when(awareStore.listAuthorized(params, context, authorization))
                    .thenReturn(storedResult);
            RequestHandler awareHandler = handlerWith(awareStore, authorization);

            assertEquals(storedResult, awareHandler.onListTasks(params, context));
            verify(awareStore).listAuthorized(params, context, authorization);
            verify(awareStore, never()).list(any(ListTasksParams.class), eq(context));
        }

        @Test
        void listTasks_failsWhenStoreOverrideReturnsDeniedTasks() {
            Task denied = saveTask("task-denied-by-override");
            TaskStore faultyStore = mock(TaskStore.class);
            TaskAuthorizationProvider authorization = mock(TaskAuthorizationProvider.class);
            ServerCallContext context = contextForUser("anyone");
            ListTasksParams params = new ListTasksParams(1, null);
            when(authorization.checkRead(context, "", TaskOperation.LIST_TASKS)).thenReturn(true);
            when(authorization.checkRead(context, denied.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(false);
            when(faultyStore.listAuthorized(params, context, authorization))
                    .thenReturn(new ListTasksResult(List.of(denied), 1, 1, "next-page"));
            RequestHandler guardedHandler = handlerWith(faultyStore, authorization);

            assertThrows(
                    TaskStoreException.class, () -> guardedHandler.onListTasks(params, context));
        }

        @Test
        void listTasks_stopsWhenStoreNeverFinishesPagination() {
            Task task = saveTask("task-1");
            TaskStore endlessStore = mock(TaskStore.class, CALLS_REAL_METHODS);
            ServerCallContext context = contextForUser("anyone");
            TaskAuthorizationProvider authorization = mock(TaskAuthorizationProvider.class);
            when(authorization.checkRead(context, "", TaskOperation.LIST_TASKS)).thenReturn(true);
            when(authorization.checkRead(context, task.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(true);
            when(endlessStore.list(any(ListTasksParams.class), eq(context)))
                    .thenReturn(new ListTasksResult(List.of(task), 1, 1, "same-token"));
            RequestHandler boundedHandler = handlerWith(endlessStore, authorization);

            assertThrows(
                    TaskStoreException.class,
                    () -> boundedHandler.onListTasks(new ListTasksParams(1, null), context));
            verify(endlessStore, atMost(2)).list(any(ListTasksParams.class), eq(context));
        }

        @Test
        void listTasks_defaultStoreMethodFiltersAndPaginates() throws A2AError {
            Task first = saveTask("task-allowed-1");
            Task denied = saveTask("task-denied");
            Task second = saveTask("task-allowed-2");
            List<Task> candidates = List.of(first, denied, second);
            TaskStore customStore = mock(TaskStore.class, CALLS_REAL_METHODS);
            ServerCallContext context = contextForUser("anyone");
            when(customStore.list(any(ListTasksParams.class), eq(context)))
                    .thenAnswer(
                            invocation -> {
                                ListTasksParams query = invocation.getArgument(0);
                                int start =
                                        query.pageToken() == null
                                                ? 0
                                                : Integer.parseInt(query.pageToken());
                                int end =
                                        Math.min(
                                                start + query.getEffectivePageSize(),
                                                candidates.size());
                                return new ListTasksResult(
                                        candidates.subList(start, end),
                                        candidates.size(),
                                        end - start,
                                        end < candidates.size() ? Integer.toString(end) : null);
                            });
            TaskAuthorizationProvider authorization = mock(TaskAuthorizationProvider.class);
            when(authorization.checkRead(context, "", TaskOperation.LIST_TASKS)).thenReturn(true);
            when(authorization.checkRead(context, first.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(true);
            when(authorization.checkRead(context, denied.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(false);
            when(authorization.checkRead(context, second.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(true);
            RequestHandler customHandler = handlerWith(customStore, authorization);

            ListTasksResult firstPage =
                    customHandler.onListTasks(new ListTasksParams(1, null), context);
            ListTasksResult secondPage =
                    customHandler.onListTasks(
                            new ListTasksParams(1, firstPage.nextPageToken()), context);

            assertEquals(List.of(first), firstPage.tasks());
            assertEquals(2, firstPage.totalSize());
            assertNotNull(firstPage.nextPageToken());
            assertEquals(List.of(second), secondPage.tasks());
            assertEquals(2, secondPage.totalSize());
            assertNull(secondPage.nextPageToken());
        }

        @Test
        void listTasks_enforcesReadForListScopeAndEachReturnedTask() throws A2AError {
            Task first = saveTask("task-1");
            Task second = saveTask("task-2");
            TaskAuthorizationProvider authorization = mock(TaskAuthorizationProvider.class);
            ServerCallContext context = contextForUser("anyone");
            when(authorization.checkRead(context, "", TaskOperation.LIST_TASKS)).thenReturn(true);
            when(authorization.checkRead(context, first.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(true);
            when(authorization.checkRead(context, second.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(true);
            RequestHandler enforcingHandler = handlerWith(taskStore, authorization);

            ListTasksResult result = enforcingHandler.onListTasks(new ListTasksParams(), context);

            assertEquals(2, result.tasks().size());
            verify(authorization, atLeastOnce()).checkRead(context, "", TaskOperation.LIST_TASKS);
            verify(authorization, atLeastOnce())
                    .checkRead(context, first.id(), TaskOperation.LIST_TASKS);
            verify(authorization, atLeastOnce())
                    .checkRead(context, second.id(), TaskOperation.LIST_TASKS);
        }

        @Test
        void listTasks_builderPathFiltersTasksDeniedByPerTaskReadAuthorization() throws A2AError {
            Task allowed = saveTask("task-allowed");
            Task denied = saveTask("task-denied");
            TaskAuthorizationProvider authorization = mock(TaskAuthorizationProvider.class);
            ServerCallContext context = contextForUser("anyone");
            when(authorization.checkRead(context, "", TaskOperation.LIST_TASKS)).thenReturn(true);
            when(authorization.checkRead(context, allowed.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(true);
            when(authorization.checkRead(context, denied.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(false);
            RequestHandler enforcingHandler = handlerWith(taskStore, authorization);

            ListTasksResult result = enforcingHandler.onListTasks(new ListTasksParams(), context);

            assertEquals(List.of(allowed), result.tasks());
            verify(authorization, atLeastOnce())
                    .checkRead(context, denied.id(), TaskOperation.LIST_TASKS);
        }

        @Test
        void listTasks_builderPathCountsAuthorizedTasksAcrossPages() throws A2AError {
            Task firstAllowed = saveTask("task-allowed-1");
            Task denied = saveTask("task-denied");
            Task secondAllowed = saveTask("task-allowed-2");
            TaskAuthorizationProvider authorization = mock(TaskAuthorizationProvider.class);
            ServerCallContext context = contextForUser("anyone");
            when(authorization.checkRead(context, "", TaskOperation.LIST_TASKS)).thenReturn(true);
            when(authorization.checkRead(context, firstAllowed.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(true);
            when(authorization.checkRead(context, denied.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(false);
            when(authorization.checkRead(context, secondAllowed.id(), TaskOperation.LIST_TASKS))
                    .thenReturn(true);
            RequestHandler enforcingHandler = handlerWith(taskStore, authorization);

            ListTasksResult firstPage =
                    enforcingHandler.onListTasks(new ListTasksParams(1, null), context);
            assertNotNull(firstPage.nextPageToken());
            ListTasksResult secondPage =
                    enforcingHandler.onListTasks(
                            new ListTasksParams(1, firstPage.nextPageToken()), context);

            assertEquals(2, firstPage.totalSize());
            assertEquals(2, secondPage.totalSize());
            assertEquals(1, firstPage.pageSize());
            assertEquals(1, secondPage.pageSize());
            assertEquals(
                    Set.of(firstAllowed, secondAllowed),
                    Set.of(firstPage.tasks().get(0), secondPage.tasks().get(0)));
            assertNull(secondPage.nextPageToken());
        }

        @Test
        void listTasks_denied() throws A2AError {
            SimpleTaskAuthorizationProvider denyListProvider =
                    new SimpleTaskAuthorizationProvider() {
                        @Override
                        public boolean checkRead(
                                ServerCallContext context, String taskId, TaskOperation operation) {
                            if (operation == TaskOperation.LIST_TASKS && taskId.isEmpty()) {
                                return false;
                            }
                            return super.checkRead(context, taskId, operation);
                        }
                    };

            RequestHandler restrictedHandler = handlerWith(taskStore, denyListProvider);

            assertThrows(
                    TaskNotFoundError.class,
                    () ->
                            restrictedHandler.onListTasks(
                                    new ListTasksParams(), contextForUser("anyone")));
        }

        @Test
        void listTasks_perTaskFiltering_nonOwnerSeesNothing() throws A2AError {
            saveTaskOwnedBy("userA");

            ListTasksResult result =
                    handler.onListTasks(new ListTasksParams(), contextForUser("userB"));

            assertEquals(0, result.tasks().size());
        }

        @Test
        void listTasks_perTaskFiltering_ownerSeesOwnTasks() throws A2AError {
            saveTaskOwnedBy("userA");
            saveTaskOwnedBy("userB");

            ListTasksResult resultA =
                    handler.onListTasks(new ListTasksParams(), contextForUser("userA"));
            ListTasksResult resultB =
                    handler.onListTasks(new ListTasksParams(), contextForUser("userB"));

            assertEquals(1, resultA.tasks().size());
            assertEquals(1, resultB.tasks().size());
        }
    }

    private Task saveTask(String taskId) {
        Task task =
                Task.builder()
                        .id(taskId)
                        .contextId("ctx-" + taskId)
                        .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                        .history(Collections.emptyList())
                        .artifacts(Collections.emptyList())
                        .build();
        taskStore.save(task, false);
        return task;
    }

    private Task saveTaskOwnedBy(String owner) {
        String taskId = "task-" + owner + "-" + System.nanoTime();
        Task task =
                Task.builder()
                        .id(taskId)
                        .contextId("ctx-" + taskId)
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
        Task task =
                Task.builder()
                        .id(taskId)
                        .contextId("ctx-" + taskId)
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
