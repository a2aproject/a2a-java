package org.a2aproject.sdk.server.requesthandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.MessageSendConfiguration;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultRequestHandlerReferenceTaskAuthorizationTest {

    private static final MessageSendConfiguration DEFAULT_CONFIG = MessageSendConfiguration.builder()
            .returnImmediately(true)
            .acceptedOutputModes(List.of())
            .build();

    private static final PushNotificationSender NOOP_PUSH_SENDER = (event, snapshot) -> {};

    private InMemoryTaskStore taskStore;
    private InMemoryQueueManager queueManager;
    private MainEventBusProcessor mainEventBusProcessor;
    private ExecutorService executor;
    private RequestHandler requestHandler;
    private AtomicReference<List<Task>> capturedRelatedTasks;
    private SimpleTaskAuthorizationProvider authProvider;

    @BeforeEach
    void setUp() {
        capturedRelatedTasks = new AtomicReference<>();
        authProvider = new SimpleTaskAuthorizationProvider();

        AgentExecutor agentExecutor = capturingAgentExecutor(capturedRelatedTasks);

        taskStore = new InMemoryTaskStore();
        MainEventBus mainEventBus = new MainEventBus();
        queueManager = new InMemoryQueueManager(taskStore, mainEventBus);
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, NOOP_PUSH_SENDER, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);

        executor = Executors.newCachedThreadPool();
        requestHandler = DefaultRequestHandler.builder()
                .agentExecutor(agentExecutor)
                .taskStore(taskStore)
                .queueManager(queueManager)
                .pushConfigStore(new InMemoryPushNotificationConfigStore())
                .mainEventBusProcessor(mainEventBusProcessor)
                .executor(executor)
                .eventConsumerExecutor(executor)
                .authorizationProvider(authProvider)
                .populateReferredTasks(true)
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

    @Test
    void ownerCanAccessOwnTasksViaReferenceTaskIds() throws Exception {
        Task ownedTask = saveTaskOwnedBy("userA");

        ServerCallContext contextA = contextForUser("userA");
        sendMessageWithReferences(List.of(ownedTask.id()), contextA);

        assertEquals(1, capturedRelatedTasks.get().size());
        assertEquals(ownedTask.id(), capturedRelatedTasks.get().get(0).id());
    }

    @Test
    void nonOwnerCannotAccessTasksViaReferenceTaskIds() {
        Task taskOwnedByA = saveTaskOwnedBy("userA");

        ServerCallContext contextB = contextForUser("userB");
        assertThrows(TaskNotFoundError.class,
                () -> sendMessageWithReferences(List.of(taskOwnedByA.id()), contextB));
    }

    @Test
    void mixedOwnership_rejectsRequestWhenAnyTaskUnauthorized() {
        Task taskA = saveTaskOwnedBy("userA");
        Task taskB = saveTaskOwnedBy("userB");

        ServerCallContext contextA = contextForUser("userA");
        assertThrows(TaskNotFoundError.class,
                () -> sendMessageWithReferences(List.of(taskA.id(), taskB.id()), contextA));
    }

    @Test
    void noAuthorizationProvider_loadsAllReferencedTasks() throws Exception {
        AtomicReference<List<Task>> localCapture = new AtomicReference<>();
        RequestHandler handlerNoAuth = DefaultRequestHandler.builder()
                .agentExecutor(capturingAgentExecutor(localCapture))
                .taskStore(taskStore)
                .queueManager(queueManager)
                .pushConfigStore(new InMemoryPushNotificationConfigStore())
                .mainEventBusProcessor(mainEventBusProcessor)
                .executor(executor)
                .eventConsumerExecutor(executor)
                .populateReferredTasks(true)
                .authorizationRequired(false)
                .build();

        Task task1 = Task.builder()
                .id("t1").contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .build();
        Task task2 = Task.builder()
                .id("t2").contextId("ctx-2")
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .build();
        taskStore.save(task1, false);
        taskStore.save(task2, false);

        Message message = Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("test")))
                .referenceTaskIds(List.of("t1", "t2"))
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(DEFAULT_CONFIG)
                .build();

        handlerNoAuth.onMessageSend(params, contextForUser("anyone"));

        assertEquals(2, localCapture.get().size());
    }

    @Nested
    class ValidateRequestedTaskAuthorizationTests {

        @Test
        void ownerCanValidateOwnActiveTask() throws A2AError {
            Task task = saveActiveTaskOwnedBy("userA");

            requestHandler.authorizeTaskAccess(task.id(), contextForUser("userA"), TaskOperation.SUBSCRIBE_TO_TASK);
        }

        @Test
        void nonOwnerCannotValidateTask() {
            Task task = saveActiveTaskOwnedBy("userA");

            assertThrows(TaskNotFoundError.class,
                    () -> requestHandler.authorizeTaskAccess(task.id(), contextForUser("userB"),
                            TaskOperation.SUBSCRIBE_TO_TASK));
        }

        @Test
        void nullTaskId_skipsAuthorizationCheck() throws A2AError {
            requestHandler.authorizeTaskAccess(null, contextForUser("anyone"), TaskOperation.SUBSCRIBE_TO_TASK);
        }

        private Task saveActiveTaskOwnedBy(String owner) {
            String taskId = "active-task-" + owner + "-" + System.nanoTime();
            Task task = Task.builder()
                    .id(taskId).contextId("ctx-" + taskId)
                    .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                    .build();
            taskStore.save(task, false);
            authProvider.recordOwnership(contextForUser(owner), taskId, TaskOperation.MESSAGE_SEND);
            return task;
        }
    }

    private Task saveTaskOwnedBy(String owner) {
        String taskId = "task-" + owner + "-" + System.nanoTime();
        Task task = Task.builder()
                .id(taskId).contextId("ctx-" + taskId)
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .build();
        taskStore.save(task, false);
        authProvider.recordOwnership(contextForUser(owner), taskId, TaskOperation.MESSAGE_SEND);
        return task;
    }

    private void sendMessageWithReferences(List<String> referenceTaskIds, ServerCallContext context) throws A2AError {
        Message message = Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("test")))
                .referenceTaskIds(referenceTaskIds)
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(DEFAULT_CONFIG)
                .build();
        requestHandler.onMessageSend(params, context);
    }

    private static AgentExecutor capturingAgentExecutor(AtomicReference<List<Task>> capture) {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                capture.set(List.copyOf(context.getRelatedTasks()));
                agentEmitter.complete();
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
            }
        };
    }

    private static ServerCallContext contextForUser(String username) {
        return new ServerCallContext(new TestUser(username), Map.of(), Set.of());
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
