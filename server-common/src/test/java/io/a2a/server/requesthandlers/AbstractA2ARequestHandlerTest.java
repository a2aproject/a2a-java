package io.a2a.server.requesthandlers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import jakarta.enterprise.context.Dependent;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.Event;
import io.a2a.spec.TextPart;
import io.a2a.util.Utils;
import io.quarkus.arc.profile.IfBuildProfile;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

public class AbstractA2ARequestHandlerTest {

    protected static final AgentCard CARD = createAgentCard(true, true, true);

    protected static final Task MINIMAL_TASK = new Task.Builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    protected static final Message MESSAGE = new Message.Builder()
            .messageId("111")
            .role(Message.Role.AGENT)
            .parts(new TextPart("test message"))
            .build();
    private static final String PREFERRED_TRANSPORT = "preferred-transport";
    private static final String A2A_REQUESTHANDLER_TEST_PROPERTIES = "/a2a-requesthandler-test.properties";

    protected AgentExecutor executor;
    protected TaskStore taskStore;
    protected RequestHandler requestHandler;
    protected AgentExecutorMethod agentExecutorExecute;
    protected AgentExecutorMethod agentExecutorCancel;
    protected InMemoryQueueManager queueManager;
    protected TestHttpClient httpClient;

    protected final Executor internalExecutor = Executors.newCachedThreadPool();

    @BeforeEach
    public void init() {
        executor = new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                if (agentExecutorExecute != null) {
                    agentExecutorExecute.invoke(context, eventQueue);
                }
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                if (agentExecutorCancel != null) {
                    agentExecutorCancel.invoke(context, eventQueue);
                }
            }
        };

        InMemoryTaskStore inMemoryTaskStore = new InMemoryTaskStore();
        taskStore = inMemoryTaskStore;
        queueManager = new InMemoryQueueManager(inMemoryTaskStore);
        httpClient = new TestHttpClient();
        PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
        PushNotificationSender pushSender = new BasePushNotificationSender(pushConfigStore, httpClient);

        requestHandler = new DefaultRequestHandler(executor, taskStore, queueManager, pushConfigStore, pushSender, internalExecutor);
    }

    @AfterEach
    public void cleanup() {
        agentExecutorExecute = null;
        agentExecutorCancel = null;
    }

    protected static AgentCard createAgentCard(boolean streaming, boolean pushNotifications, boolean stateTransitionHistory) {
        String preferredTransport = loadPreferredTransportFromProperties();
        AgentCard.Builder builder = new AgentCard.Builder()
                .name("test-card")
                .description("A test agent card")
                .url("http://example.com")
                .version("1.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(streaming)
                        .pushNotifications(pushNotifications)
                        .stateTransitionHistory(stateTransitionHistory)
                        .build())
                .defaultInputModes(new ArrayList<>())
                .defaultOutputModes(new ArrayList<>())
                .preferredTransport(preferredTransport)
                .skills(new ArrayList<>())
                .protocolVersion("0.2.5");
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
        void invoke(RequestContext context, EventQueue eventQueue) throws JSONRPCError;
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
                tasks.add(Utils.OBJECT_MAPPER.readValue(body, Task.TYPE_REFERENCE));
                try {
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
                } finally {
                    latch.countDown();
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
