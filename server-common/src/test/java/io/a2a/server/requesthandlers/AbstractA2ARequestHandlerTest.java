package io.a2a.server.requesthandlers;

import static io.a2a.spec.AgentCard.CURRENT_PROTOCOL_VERSION;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import jakarta.enterprise.context.Dependent;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.EventQueueUtil;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.MainEventBus;
import io.a2a.server.events.MainEventBusProcessor;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.A2AError;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import io.quarkus.arc.profile.IfBuildProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

public class AbstractA2ARequestHandlerTest {

    protected static final AgentCard CARD = createAgentCard(true, true, true);

    protected static final Task MINIMAL_TASK = Task.builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    protected static final Message MESSAGE = Message.builder()
            .messageId("111")
            .role(Message.Role.AGENT)
            .parts(new TextPart("test message"))
            .build();
    private static final String PREFERRED_TRANSPORT = "preferred-transport";
    private static final String A2A_REQUESTHANDLER_TEST_PROPERTIES = "/a2a-requesthandler-test.properties";

    private static final PushNotificationSender NOOP_PUSHNOTIFICATION_SENDER = task -> {};

    protected AgentExecutor executor;
    protected TaskStore taskStore;
    protected RequestHandler requestHandler;
    protected AgentExecutorMethod agentExecutorExecute;
    protected AgentExecutorMethod agentExecutorCancel;
    protected InMemoryQueueManager queueManager;
    protected TestHttpClient httpClient;
    protected MainEventBus mainEventBus;
    protected MainEventBusProcessor mainEventBusProcessor;

    protected final Executor internalExecutor = Executors.newCachedThreadPool();

    @BeforeEach
    public void init() {
        executor = new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws A2AError {
                if (agentExecutorExecute != null) {
                    agentExecutorExecute.invoke(context, eventQueue);
                }
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws A2AError {
                if (agentExecutorCancel != null) {
                    agentExecutorCancel.invoke(context, eventQueue);
                }
            }
        };

        InMemoryTaskStore inMemoryTaskStore = new InMemoryTaskStore();
        taskStore = inMemoryTaskStore;

        // Create push notification components BEFORE MainEventBusProcessor
        httpClient = new TestHttpClient();
        PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
        PushNotificationSender pushSender = new BasePushNotificationSender(pushConfigStore, httpClient);

        // Create MainEventBus and MainEventBusProcessor (production code path)
        mainEventBus = new MainEventBus();
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, pushSender);
        EventQueueUtil.start(mainEventBusProcessor);

        queueManager = new InMemoryQueueManager(inMemoryTaskStore, mainEventBus);

        requestHandler = DefaultRequestHandler.create(
                executor, taskStore, queueManager, pushConfigStore, mainEventBusProcessor, internalExecutor, internalExecutor);
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

    protected static AgentCard createAgentCard(boolean streaming, boolean pushNotifications, boolean stateTransitionHistory) {
        String preferredTransport = loadPreferredTransportFromProperties();
        AgentCard.Builder builder = AgentCard.builder()
                .name("test-card")
                .description("A test agent card")
                .supportedInterfaces(Collections.singletonList(new AgentInterface(preferredTransport, "http://example.com")))
                .version("1.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(AgentCapabilities.builder()
                        .streaming(streaming)
                        .pushNotifications(pushNotifications)
                        .stateTransitionHistory(stateTransitionHistory)
                        .build())
                .defaultInputModes(new ArrayList<>())
                .defaultOutputModes(new ArrayList<>())
                .skills(new ArrayList<>())
                .protocolVersions(CURRENT_PROTOCOL_VERSION);
        return builder.build();
    }

    private static String loadPreferredTransportFromProperties() {
        URL url = AbstractA2ARequestHandlerTest.class.getResource(A2A_REQUESTHANDLER_TEST_PROPERTIES);
        Assertions.assertNotNull(url);
        Properties properties = new Properties();
        try {
            try (InputStream in = url.openStream()){
                properties.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String preferredTransport = properties.getProperty(PREFERRED_TRANSPORT);
        Assertions.assertNotNull(preferredTransport);
        return preferredTransport;
    }

    protected interface AgentExecutorMethod {
        void invoke(RequestContext context, EventQueue eventQueue) throws A2AError;
    }

    /**
     * Helper method to wrap events in EventQueueItem for tests.
     * Creates a simple wrapper that marks events as non-replicated (local events).
     */
    protected static EventQueueItem wrapEvent(Event event) {
        return new EventQueueItem() {
            @Override
            public Event getEvent() {
                return event;
            }

            @Override
            public boolean isReplicated() {
                return false;
            }
        };
    }

    @Dependent
    @IfBuildProfile("test")
    protected static class TestHttpClient implements A2AHttpClient {
        public final List<Task> tasks = Collections.synchronizedList(new ArrayList<>());
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
                    Task task = JsonUtil.fromJson(body, Task.class);
                    tasks.add(task);
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
                    throw new IOException("Failed to parse task JSON", e);
                } finally {
                    if (latch != null) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public CompletableFuture<Void> postAsyncSSE(Consumer<String> messageConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
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
