package org.a2aproject.sdk.compat03.conversion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import jakarta.enterprise.context.Dependent;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.events.EventQueueUtil;
import org.a2aproject.sdk.server.events.InMemoryQueueManager;
import org.a2aproject.sdk.server.events.MainEventBus;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.server.tasks.BasePushNotificationSender;
import org.a2aproject.sdk.server.tasks.InMemoryPushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.PushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import io.quarkus.arc.profile.IfBuildProfile;

// V0.3 imports for test fixtures
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;

/**
 * Base test class for v0.3 transport handler tests.
 * <p>
 * This class sets up the test infrastructure that bridges v0.3 transport handlers
 * to the v1.0 backend via the {@link Convert_v0_3_To10RequestHandler} conversion layer.
 * </p>
 *
 * <h2>Architecture:</h2>
 * <pre>
 * Test (v0.3 types) → Handler (v0.3) → Convert03To10RequestHandler → v1.0 Backend
 * </pre>
 *
 * <h2>Test fixtures:</h2>
 * All test fixtures use v0.3 types ({@code org.a2aproject.sdk.compat03.spec.*})
 * to match the client perspective.
 *
 * <h2>Backend:</h2>
 * The backend uses v1.0 components ({@code org.a2aproject.sdk.server.*}) with
 * the conversion happening transparently in {@link Convert_v0_3_To10RequestHandler}.
 */
public abstract class AbstractA2ARequestHandlerTest_v0_3 {

    // V0.3 test fixtures (client perspective)
    protected static final AgentCard_v0_3 CARD = createAgentCard(true, true, true);

    protected static final Task_v0_3 MINIMAL_TASK = new Task_v0_3.Builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus_v0_3(TaskState_v0_3.SUBMITTED))
            .build();

    protected static final Message_v0_3 MESSAGE = new Message_v0_3.Builder()
            .messageId("111")
            .role(Message_v0_3.Role.AGENT)
            .parts(new TextPart_v0_3("test message"))
            .build();

    private static final PushNotificationSender NOOP_PUSHNOTIFICATION_SENDER = task -> {};

    // V1.0 backend infrastructure
    protected AgentExecutor agentExecutor;
    protected TaskStore taskStore;
    protected InMemoryQueueManager queueManager;
    protected TestHttpClient httpClient;
    protected MainEventBus mainEventBus;
    protected MainEventBusProcessor mainEventBusProcessor;

    // V0.3 conversion layer (what transport handlers use)
    protected Convert_v0_3_To10RequestHandler convert03To10Handler;

    // Lambda injection for AgentExecutor behavior (v0.3.x pattern)
    protected AgentExecutorMethod agentExecutorExecute;
    protected AgentExecutorMethod agentExecutorCancel;

    protected final Executor internalExecutor = Executors.newCachedThreadPool();

    @BeforeEach
    public void init() {
        // Create AgentExecutor with lambda injection (v1.0 interface)
        agentExecutor = new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                if (agentExecutorExecute != null) {
                    agentExecutorExecute.invoke(context, agentEmitter);
                }
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                if (agentExecutorCancel != null) {
                    agentExecutorCancel.invoke(context, agentEmitter);
                }
            }
        };

        // Set up v1.0 backend components
        InMemoryTaskStore inMemoryTaskStore = new InMemoryTaskStore();
        taskStore = inMemoryTaskStore;

        // Create push notification components BEFORE MainEventBusProcessor
        httpClient = new TestHttpClient();
        PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
        PushNotificationSender pushSender = new BasePushNotificationSender(pushConfigStore, httpClient);

        // Create MainEventBus and MainEventBusProcessor (production code path)
        mainEventBus = new MainEventBus();
        queueManager = new InMemoryQueueManager(inMemoryTaskStore, mainEventBus);
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, pushSender, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);

        // Create v1.0 DefaultRequestHandler
        org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler v10Handler =
                DefaultRequestHandler.create(
                        agentExecutor, taskStore, queueManager, pushConfigStore,
                        mainEventBusProcessor, internalExecutor, internalExecutor);

        // Wrap in v0.3 conversion handler
        convert03To10Handler = new Convert_v0_3_To10RequestHandler();
        convert03To10Handler.v10Handler = v10Handler;
    }

    @AfterEach
    public void cleanup() {
        agentExecutorExecute = null;
        agentExecutorCancel = null;

        // Stop MainEventBusProcessor background thread
        if (mainEventBusProcessor != null) {
            EventQueueUtil.stop(mainEventBusProcessor);
        }
    }

    /**
     * Creates a v0.3 AgentCard with specified capabilities.
     *
     * @param streaming whether streaming is supported
     * @param pushNotifications whether push notifications are supported
     * @param stateTransitionHistory whether state transition history is supported
     * @return configured AgentCard
     */
    protected static AgentCard_v0_3 createAgentCard(boolean streaming, boolean pushNotifications,
                                                    boolean stateTransitionHistory) {
        return new AgentCard_v0_3.Builder()
                .name("test-card")
                .description("A test agent card")
                .url("http://example.com")
                .version("1.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities_v0_3.Builder()
                        .streaming(streaming)
                        .pushNotifications(pushNotifications)
                        .stateTransitionHistory(stateTransitionHistory)
                        .build())
                .defaultInputModes(new ArrayList<>())
                .defaultOutputModes(new ArrayList<>())
                .preferredTransport("jsonrpc")
                .skills(new ArrayList<>())
                .protocolVersion("0.3")
                .build();
    }

    /**
     * Lambda interface for AgentExecutor method injection in tests.
     */
    protected interface AgentExecutorMethod {
        void invoke(RequestContext context, AgentEmitter agentEmitter) throws A2AError;
    }

    /**
     * Test HTTP client for push notification testing.
     * Captures posted events for verification.
     */
    @Dependent
    @IfBuildProfile("test")
    protected static class TestHttpClient implements A2AHttpClient {
        public final List<StreamingEventKind> events = Collections.synchronizedList(new ArrayList<>());
        public volatile CountDownLatch latch;

        @Override
        public GetBuilder createGet() {
            return null;
        }

        @Override
        public PostBuilder createPost() {
            return new TestHttpClient.TestPostBuilder();
        }

        @Override
        public DeleteBuilder createDelete() {
            return null;
        }

        class TestPostBuilder implements A2AHttpClient.PostBuilder {
            private volatile String body;

            @Override
            public PostBuilder body(String body) {
                this.body = body;
                return this;
            }

            @Override
            public A2AHttpResponse post() throws IOException, InterruptedException {
                try {
                    // Parse StreamResponse format to extract the streaming event
                    // The body contains a wrapper with one of: task, message, statusUpdate, artifactUpdate
                    StreamingEventKind event = JsonUtil.fromJson(body, StreamingEventKind.class);
                    events.add(event);
                    return new A2AHttpResponse() {
                        @Override
                        public int status() {
                            return 200;
                        }

                        @Override
                        public boolean success() {
                            return true;
                        }

                        @Override
                        public String body() {
                            return "";
                        }
                    };
                } catch (JsonProcessingException e) {
                    throw new IOException("Failed to parse StreamingEventKind JSON", e);
                } finally {
                    if (latch != null) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public CompletableFuture<Void> postAsyncSSE(Consumer<String> messageConsumer,
                                                        Consumer<Throwable> errorConsumer,
                                                        Runnable completeRunnable)
                    throws IOException, InterruptedException {
                return null;
            }

            @Override
            public PostBuilder url(String s) {
                return this;
            }

            @Override
            public PostBuilder addHeader(String name, String value) {
                return this;
            }

            @Override
            public PostBuilder addHeaders(Map<String, String> headers) {
                return this;
            }
        }
    }
}
