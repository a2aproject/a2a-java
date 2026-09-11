package org.a2aproject.sdk.itk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.MessageEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.grpc.utils.ProtoJsonUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code tck-client-parse} behaviour: ACTS §10 client tests.
 *
 * <p>Every other ACTS step drives the SUT as a server — send bytes, assert on what comes back. A
 * client test inverts that: it supplies a canonical wire payload and asks whether this SDK's
 * <em>client</em> parses it correctly, which no A2A operation can ask of a server. §10 defines the
 * file format and says nothing about the mechanism, so without a convention like this one the
 * runner cannot reach the client at all and skips the CLIENT-* tests.
 *
 * <p>The runner sends an ordinary {@code send_message} naming this behaviour with
 * {@code {operation, wire_payload}} in a data part; the agent builds a real client whose HTTP
 * transport returns that payload verbatim, performs the operation, and hands back whatever its own
 * client produced.
 *
 * <p>A stubbed {@link A2AHttpClient} rather than a bare deserializer: decoding the payload straight
 * into spec types would be a fraction of the code and would prove much less, skipping the JSON-RPC
 * envelope, the error mapping and the response plumbing that are most of what a client test is
 * about. CLIENT-PARSE-004 makes that concrete — it feeds an error envelope and expects
 * {@code {error: {code, message}}}.
 */
final class ActsClientParse {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActsClientParse.class);

    static final String BEHAVIOR = "tck-client-parse";

    /** Nothing dials this — the stub answers before a socket is opened — but the client needs a syntactically valid base. */
    private static final String BASE_URL = "http://acts-client-parse.invalid";

    private static final Gson GSON = new Gson();

    /**
     * The printer the SDK itself uses on the wire (see {@code JSONRPCUtils}), minus the whitespace
     * option, which only affects readability. {@code alwaysPrintFieldsWithNoPresence} matters:
     * without it a capability the client correctly parsed as {@code false} vanishes from the
     * rendering, and CLIENT-CAP-001 asserts on exactly that.
     */
    private static final JsonFormat.Printer PRINTER = JsonFormat.printer().alwaysPrintFieldsWithNoPresence();

    void run(RequestContext context, AgentEmitter emitter) {
        Map<String, Object> request = requestIn(context.getMessage());
        if (request == null) {
            emitter.updateStatus(TaskState.TASK_STATE_FAILED, emitter.newAgentMessage(
                    List.of(new TextPart(BEHAVIOR + " needs {operation, wire_payload}")), null));
            return;
        }

        String operation = String.valueOf(request.get("operation"));
        String payload = GSON.toJson(withIntegralNumbers(request.get("wire_payload")));
        LOGGER.info("Parsing an ACTS {} payload through this SDK's own client", operation);

        Map<String, Object> parsed = parse(operation, payload);
        emitter.addArtifact(List.of(new DataPart(parsed)), null, BEHAVIOR, null);
        emitter.updateStatus(TaskState.TASK_STATE_COMPLETED,
                emitter.newAgentMessage(List.of(new TextPart(operation + " parsed")), null));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestIn(Message message) {
        if (message == null || message.parts() == null) {
            return null;
        }
        for (Part<?> part : message.parts()) {
            if (part instanceof DataPart dataPart && dataPart.data() instanceof Map<?, ?> data
                    && data.get("operation") instanceof String) {
                return (Map<String, Object>) data;
            }
        }
        return null;
    }

    /**
     * Restores integers the transport turned into doubles.
     *
     * <p>A {@code DataPart} rides the wire as a {@code google.protobuf.Value}, whose only numeric
     * kind is {@code double}, so the corpus's {@code code: -32001} reaches us as {@code -32001.0}
     * and Gson would re-emit it that way, leaving the client to coerce a fractional literal into an
     * {@code int32} field. The corpus wrote an integer; this puts one back.
     */
    private Object withIntegralNumbers(Object value) {
        if (value instanceof Double number && !number.isInfinite() && number == Math.rint(number)) {
            return number.longValue();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, entry) -> copy.put(String.valueOf(key), withIntegralNumbers(entry)));
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(entry -> copy.add(withIntegralNumbers(entry)));
            return copy;
        }
        return value;
    }

    private Map<String, Object> parse(String operation, String payload) {
        try {
            return switch (operation) {
                // Both card operations land here when the payload is a bare card, which is how the
                // corpus writes them — correctly, since a card is fetched over plain HTTP on every
                // binding, so there is no envelope to unwrap.
                case "get_agent_card" -> wire(ProtoUtils.ToProto.agentCard(parseCard(payload)));
                case "get_extended_agent_card" -> wire(ProtoUtils.ToProto.agentCard(parseExtendedCard(payload)));
                case "send_message" -> wire(ProtoUtils.ToProto.taskOrMessage(parseSendMessage(payload)));
                case "get_task" -> wire(ProtoUtils.ToProto.task(parseGetTask(payload)));
                default -> error("unsupported client operation \"" + operation + "\"", null);
            };
        } catch (Exception e) {
            return raised(e, payload);
        }
    }

    private AgentCard parseCard(String payload) throws Exception {
        return A2A.getAgentCard(new FixedHttpClient(payload), BASE_URL);
    }

    private AgentCard parseExtendedCard(String payload) throws Exception {
        // The corpus writes this as a bare card, matching the wire: its own payload names REST,
        // where the extended card is a plain GET. Accept an envelope too, since JSON-RPC wraps it.
        if (!isEnveloped(payload)) {
            return parseCard(payload);
        }
        try (Client client = client(payload, new AtomicReference<>())) {
            return client.getExtendedAgentCard();
        }
    }

    private EventKind parseSendMessage(String payload) throws Exception {
        AtomicReference<ClientEvent> captured = new AtomicReference<>();
        try (Client client = client(payload, captured)) {
            client.sendMessage(A2A.toUserMessage("acts"));
        }
        ClientEvent event = captured.get();
        if (event instanceof MessageEvent messageEvent) {
            return messageEvent.getMessage();
        }
        if (event instanceof TaskEvent taskEvent) {
            return taskEvent.getTask();
        }
        if (event instanceof TaskUpdateEvent updateEvent) {
            return updateEvent.getTask();
        }
        throw new IllegalStateException("the client produced no event");
    }

    private Task parseGetTask(String payload) throws Exception {
        try (Client client = client(payload, new AtomicReference<>())) {
            return client.getTask(new TaskQueryParams("acts"));
        }
    }

    /**
     * A real client bound to the stub transport.
     *
     * <p>Streaming is off in both the config and the synthetic card, which puts {@code sendMessage}
     * on the unary path — there the consumer fires synchronously, before the call returns.
     */
    private Client client(String payload, AtomicReference<ClientEvent> captured) throws Exception {
        return Client.builder(syntheticCard())
                .clientConfig(new ClientConfig.Builder().setStreaming(false).build())
                .withTransport(JSONRPCTransport.class,
                        new JSONRPCTransportConfigBuilder().httpClient(new FixedHttpClient(payload)))
                .addConsumer((event, card) -> captured.set(event))
                .build();
    }

    private AgentCard syntheticCard() {
        return AgentCard.builder()
                .name("ACTS client parse")
                .description("Drives this SDK's client against a canned wire payload.")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface("JSONRPC", BASE_URL)))
                .build();
    }

    private boolean isEnveloped(String payload) {
        Map<String, Object> body = envelopeOf(payload);
        return body != null
                && (body.containsKey("jsonrpc") || body.containsKey("result") || body.containsKey("error"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> envelopeOf(String payload) {
        try {
            return GSON.fromJson(payload, Map.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Renders what the client produced as A2A wire JSON.
     *
     * <p>{@code send_message} keeps its {@code SendMessageResponse} envelope because §4.2 makes the
     * {@code task}/{@code message} discriminator part of that operation's assertion root;
     * {@code get_task} and the card operations are asserted on their own fields.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> wire(MessageOrBuilder proto) throws Exception {
        return GSON.fromJson(ProtoJsonUtils.toJson(PRINTER, proto), Map.class);
    }

    /**
     * Renders a client-raised failure the way {@code expect_parsed} addresses it.
     *
     * <p>The envelope's own error wins when the payload carried one: the assertion is about the
     * client having surfaced <em>that</em> error, and inventing a code here would pass the test
     * without the client having done anything.
     */
    private Map<String, Object> raised(Exception e, String payload) {
        Map<String, Object> envelope = envelopeOf(payload);
        if (envelope != null && envelope.get("error") instanceof Map<?, ?> wire) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("error", wire);
            out.put("raised", String.valueOf(e.getMessage()));
            return out;
        }
        Integer code = e.getCause() instanceof A2AError a2aError ? a2aError.getCode() : null;
        return error(String.valueOf(e.getMessage()), code);
    }

    private Map<String, Object> error(String message, Integer code) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (code != null) {
            detail.put("code", code);
        }
        detail.put("message", message);
        return Map.of("error", detail);
    }

    /**
     * Answers every request with the payload under test.
     *
     * <p>The status is always 200: the JSON-RPC transport checks the HTTP status before it looks at
     * the body, so an error envelope served at 4xx is discarded as an unexpected HTTP status and
     * never parsed.
     */
    private final class FixedHttpClient implements A2AHttpClient {

        private final String payload;

        private FixedHttpClient(String payload) {
            this.payload = payload;
        }

        @Override
        public GetBuilder createGet() {
            return new FixedGetBuilder();
        }

        @Override
        public PostBuilder createPost() {
            return new FixedPostBuilder();
        }

        @Override
        public DeleteBuilder createDelete() {
            return new FixedDeleteBuilder();
        }

        /**
         * Rewrites the response's JSON-RPC id to the request's, which is what a real server does.
         *
         * <p>The corpus's canned payloads carry a fixed id that cannot match one the client invented
         * at call time, so a client validating the correlation rejects the payload before parsing
         * any of it — leaving the test measuring correlation rather than parsing.
         */
        private String echoingId(String sent) {
            if (!isEnveloped(payload)) {
                return payload;
            }
            Map<String, Object> envelope = envelopeOf(payload);
            Map<String, Object> request = envelopeOf(sent);
            if (envelope == null || request == null || !request.containsKey("id")) {
                return payload;
            }
            envelope.put("id", request.get("id"));
            return GSON.toJson(envelope);
        }

        private A2AHttpResponse respond(String body) {
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
                    return body;
                }
            };
        }

        /** No §10 payload is a stream; a client that asks for one has taken a path the test did not intend. */
        private CompletableFuture<Void> unsupportedStream(Consumer<Throwable> errorConsumer) {
            IllegalStateException failure = new IllegalStateException("ACTS client parse serves no stream");
            errorConsumer.accept(failure);
            return CompletableFuture.failedFuture(failure);
        }

        private final class FixedGetBuilder implements GetBuilder {
            @Override
            public GetBuilder url(String s) {
                return this;
            }

            @Override
            public GetBuilder addHeaders(Map<String, String> headers) {
                return this;
            }

            @Override
            public GetBuilder addHeader(String name, String value) {
                return this;
            }

            @Override
            public A2AHttpResponse get() {
                return respond(payload);
            }

            @Override
            public CompletableFuture<Void> getAsyncSSE(Consumer<ServerSentEvent> messageConsumer,
                                                       Consumer<Throwable> errorConsumer,
                                                       Runnable completeRunnable) {
                return unsupportedStream(errorConsumer);
            }
        }

        private final class FixedPostBuilder implements PostBuilder {
            private String sent = "";

            @Override
            public PostBuilder body(String body) {
                this.sent = body;
                return this;
            }

            @Override
            public PostBuilder url(String s) {
                return this;
            }

            @Override
            public PostBuilder addHeaders(Map<String, String> headers) {
                return this;
            }

            @Override
            public PostBuilder addHeader(String name, String value) {
                return this;
            }

            @Override
            public A2AHttpResponse post() {
                return respond(echoingId(sent));
            }

            @Override
            public CompletableFuture<Void> postAsyncSSE(Consumer<ServerSentEvent> messageConsumer,
                                                        Consumer<Throwable> errorConsumer,
                                                        Runnable completeRunnable) {
                return unsupportedStream(errorConsumer);
            }
        }

        private final class FixedDeleteBuilder implements DeleteBuilder {
            @Override
            public DeleteBuilder url(String s) {
                return this;
            }

            @Override
            public DeleteBuilder addHeaders(Map<String, String> headers) {
                return this;
            }

            @Override
            public DeleteBuilder addHeader(String name, String value) {
                return this;
            }

            @Override
            public A2AHttpResponse delete() {
                return respond(payload);
            }
        }
    }
}
