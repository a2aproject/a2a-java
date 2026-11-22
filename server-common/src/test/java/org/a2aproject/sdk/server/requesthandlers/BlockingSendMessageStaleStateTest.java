package org.a2aproject.sdk.server.requesthandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.events.EventQueueUtil;
import org.a2aproject.sdk.server.events.InMemoryQueueManager;
import org.a2aproject.sdk.server.events.MainEventBus;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.server.tasks.InMemoryPushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.server.tasks.TaskStateProvider;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * A blocking {@code SendMessage} must return the task as it stands once the agent has finished,
 * not as it stood a moment earlier.
 * <p>
 * The handler reads the task back from the {@link TaskStore} after waiting for consumption, which
 * only guarantees that the events already distributed to the consumer were persisted. An agent that
 * ends its turn in an interrupted state (INPUT_REQUIRED / AUTH_REQUIRED) enqueues one more event
 * that is still inside {@link MainEventBusProcessor} at that point, so the read used to return the
 * previous state ({@code SUBMITTED} / {@code WORKING}) without raising anything.
 * <p>
 * These tests make persistence deliberately slow so the ordering is deterministic rather than
 * timing-dependent.
 */
class BlockingSendMessageStaleStateTest {

    private static final ServerCallContext CONTEXT = null;

    private static final Message MESSAGE = Message.builder()
            .messageId("111")
            .role(Message.Role.ROLE_AGENT)
            .parts(new TextPart("test message"))
            .build();

    /** Long enough that the persist of the last event cannot finish before the handler reads. */
    private static final long SLOW_PERSIST_MILLIS = 1200;

    private final Executor internalExecutor = Executors.newCachedThreadPool();

    private MainEventBusProcessor mainEventBusProcessor;

    @AfterEach
    void stopProcessor() {
        if (mainEventBusProcessor != null) {
            EventQueueUtil.stop(mainEventBusProcessor);
            mainEventBusProcessor = null;
        }
    }

    private RequestHandler buildHandler(
            TaskStore taskStore, TaskStateProvider stateProvider, AgentExecutor agentExecutor) {
        return buildHandler(taskStore, stateProvider, agentExecutor, 30);
    }

    private RequestHandler buildHandler(TaskStore taskStore, TaskStateProvider stateProvider,
            AgentExecutor agentExecutor, int agentCompletionTimeoutSeconds) {
        MainEventBus mainEventBus = new MainEventBus();
        InMemoryQueueManager queueManager = new InMemoryQueueManager(stateProvider, mainEventBus);
        mainEventBusProcessor =
                new MainEventBusProcessor(mainEventBus, taskStore, PushNotificationSender.NOOP, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);

        return DefaultRequestHandler.builder()
                .agentExecutor(agentExecutor)
                .taskStore(taskStore)
                .queueManager(queueManager)
                .pushConfigStore(new InMemoryPushNotificationConfigStore())
                .mainEventBusProcessor(mainEventBusProcessor)
                .executor(internalExecutor)
                .eventConsumerExecutor(internalExecutor)
                .authorizationRequired(false)
                .agentCompletionTimeoutSeconds(agentCompletionTimeoutSeconds)
                .build();
    }

    private static AgentExecutor agent(AgentExecutorMethod method) {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                method.invoke(context, agentEmitter);
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                // not exercised
            }
        };
    }

    @FunctionalInterface
    private interface AgentExecutorMethod {
        void invoke(RequestContext context, AgentEmitter agentEmitter) throws A2AError;
    }

    @Test
    void blockingSendReturnsInputRequiredEvenWhenTheLastEventIsStillBeingPersisted() {
        InMemoryTaskStore backing = new InMemoryTaskStore();
        TaskStore slowStore = new SlowPersistTaskStore(backing, TaskState.TASK_STATE_INPUT_REQUIRED,
                SLOW_PERSIST_MILLIS);

        RequestHandler handler = buildHandler(slowStore, backing, agent((context, emitter) -> {
            emitter.startWork();
            emitter.requiresInput();
        }));

        EventKind result = handler.onMessageSend(MessageSendParams.builder().message(MESSAGE).build(), CONTEXT);

        assertInstanceOf(Task.class, result, "blocking send should return a Task");
        Task returned = (Task) result;
        assertEquals(TaskState.TASK_STATE_INPUT_REQUIRED, returned.status().state(),
                "the returned task must reflect the state the agent ended in, not the one before it");

        // The store is the eventual truth; the failure mode was returning something older than this.
        Task stored = backing.get(returned.id());
        assertEquals(TaskState.TASK_STATE_INPUT_REQUIRED, stored.status().state(),
                "the task should also be persisted as INPUT_REQUIRED");
    }

    @Test
    void blockingSendReturnsAuthRequiredEvenWhenTheLastEventIsStillBeingPersisted() {
        InMemoryTaskStore backing = new InMemoryTaskStore();
        TaskStore slowStore = new SlowPersistTaskStore(backing, TaskState.TASK_STATE_AUTH_REQUIRED,
                SLOW_PERSIST_MILLIS);

        RequestHandler handler = buildHandler(slowStore, backing, agent((context, emitter) -> {
            emitter.startWork();
            emitter.requiresAuth();
        }));

        EventKind result = handler.onMessageSend(MessageSendParams.builder().message(MESSAGE).build(), CONTEXT);

        assertInstanceOf(Task.class, result, "blocking send should return a Task");
        Task returned = (Task) result;
        assertEquals(TaskState.TASK_STATE_AUTH_REQUIRED, returned.status().state(),
                "AUTH_REQUIRED is the other interrupted state, and it goes through the same blocking path");
    }

    @Test
    void blockingSendOfATerminalStateIsUnaffected() {
        InMemoryTaskStore backing = new InMemoryTaskStore();
        TaskStore slowStore = new SlowPersistTaskStore(backing, TaskState.TASK_STATE_COMPLETED,
                SLOW_PERSIST_MILLIS);

        RequestHandler handler = buildHandler(slowStore, backing, agent((context, emitter) -> {
            emitter.startWork();
            emitter.complete();
        }));

        EventKind result = handler.onMessageSend(MessageSendParams.builder().message(MESSAGE).build(), CONTEXT);

        assertInstanceOf(Task.class, result, "blocking send should return a Task");
        Task returned = (Task) result;
        assertEquals(TaskState.TASK_STATE_COMPLETED, returned.status().state(),
                "terminal states were already handled before the fix and must stay correct");
    }

    @Test
    void blockingSendReturnsATrailingArtifactEvenWhenItIsStillBeingPersisted() {
        InMemoryTaskStore backing = new InMemoryTaskStore();
        TaskStore slowStore = new SlowPersistTaskStore(backing, task -> !task.artifacts().isEmpty(),
                SLOW_PERSIST_MILLIS);

        RequestHandler handler = buildHandler(slowStore, backing, agent((context, emitter) -> {
            emitter.startWork();
            emitter.addArtifact(List.of(new TextPart("answer")), "answer-id", "answer", null);
        }));

        EventKind result = handler.onMessageSend(MessageSendParams.builder().message(MESSAGE).build(), CONTEXT);

        assertInstanceOf(Task.class, result, "blocking send should return a Task");
        Task returned = (Task) result;
        assertEquals(TaskState.TASK_STATE_WORKING, returned.status().state(),
                "an artifact-only tail does not change the state");
        assertEquals(1, returned.artifacts().size(),
                "the artifact the agent added last must be in the returned task");
        assertEquals("answer-id", returned.artifacts().get(0).artifactId());
    }

    @Test
    void blockingSendDoesNotWaitForTheQueueToDrainWhenTheAgentIsStillRunning() throws Exception {
        InMemoryTaskStore backing = new InMemoryTaskStore();
        // Persisting the artifact keeps an event in flight well past the agent completion timeout
        long slowArtifactMillis = 4000;
        TaskStore slowStore = new SlowPersistTaskStore(backing, task -> !task.artifacts().isEmpty(),
                slowArtifactMillis);
        CountDownLatch agentMayFinish = new CountDownLatch(1);

        RequestHandler handler = buildHandler(slowStore, backing, agent((context, emitter) -> {
            emitter.startWork();
            emitter.addArtifact(List.of(new TextPart("partial")), "partial-id", "partial", null);
            try {
                // Still running when the handler gives up waiting for it
                agentMayFinish.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }), 1);

        try {
            long startNanos = System.nanoTime();
            EventKind result = handler.onMessageSend(MessageSendParams.builder().message(MESSAGE).build(), CONTEXT);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

            assertInstanceOf(Task.class, result, "blocking send should return a Task");
            assertEquals(TaskState.TASK_STATE_WORKING, ((Task) result).status().state());
            // 1s agent timeout plus consumption; waiting for the drain would take about 4s
            assertTrue(elapsedMillis < 3000,
                    "the handler waited for in-flight events of an agent that is still running: " + elapsedMillis + "ms");
        } finally {
            agentMayFinish.countDown();
        }
    }

    /**
     * Wraps a store and slows down the save of one particular task state, so that the last event
     * of an agent run is still being persisted when the handler reads the task back.
     */
    private static final class SlowPersistTaskStore implements TaskStore {

        private final InMemoryTaskStore delegate;
        private final Predicate<Task> slow;
        private final long delayMillis;

        SlowPersistTaskStore(InMemoryTaskStore delegate, TaskState slowState, long delayMillis) {
            this(delegate, task -> task.status() != null && task.status().state() == slowState, delayMillis);
        }

        SlowPersistTaskStore(InMemoryTaskStore delegate, Predicate<Task> slow, long delayMillis) {
            this.delegate = delegate;
            this.slow = slow;
            this.delayMillis = delayMillis;
        }

        @Override
        public void save(Task task, boolean isReplicated) {
            if (slow.test(task)) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(delayMillis));
            }
            delegate.save(task, isReplicated);
        }

        @Override
        public @Nullable Task get(String taskId) {
            return delegate.get(taskId);
        }

        @Override
        public void delete(String taskId) {
            delegate.delete(taskId);
        }

        @Override
        public ListTasksResult list(ListTasksParams params, @Nullable ServerCallContext context) {
            return delegate.list(params, context);
        }
    }
}
