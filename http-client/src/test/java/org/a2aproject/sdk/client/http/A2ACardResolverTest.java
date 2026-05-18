package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.spec.A2AClientError;
import org.a2aproject.sdk.spec.A2AClientJSONError;
import org.a2aproject.sdk.spec.AgentCard;
import org.junit.jupiter.api.Test;

public class A2ACardResolverTest {

    private static final String AGENT_CARD_PATH = "/.well-known/agent-card.json";

    private TestHttpClient createTestClient() {
        TestHttpClient client = new TestHttpClient();
        client.body = JsonMessages.AGENT_CARD;
        return client;
    }

    @Test
    public void testWellKnownUrlStripsSlashes() throws Exception {
        TestHttpClient client = createTestClient();

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").build().getWellKnownAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").build().getWellKnownAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testConfiguredAgentCardUrlNormalization() throws Exception {
        TestHttpClient client = createTestClient();

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").agentCardPath(AGENT_CARD_PATH).build().getConfiguredAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").agentCardPath(AGENT_CARD_PATH).build().getConfiguredAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").agentCardPath(AGENT_CARD_PATH.substring(1)).build().getConfiguredAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").agentCardPath(AGENT_CARD_PATH.substring(1)).build().getConfiguredAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testConfiguredAgentCardUrlDoesNotIntroduceDoubleSlash() throws Exception {
        TestHttpClient client = createTestClient();

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").agentCardPath("/custom/agent.json").build().getConfiguredAgentCard();

        assertEquals("http://example.com/custom/agent.json", client.url);
    }

    @Test
    public void testGetWellKnownAgentCardSuccess() throws Exception {
        TestHttpClient client = createTestClient();

        AgentCard card = A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").build().getWellKnownAgentCard();

        AgentCard expectedCard = unmarshalFrom(JsonMessages.AGENT_CARD);
        assertEquals(printAgentCard(expectedCard), printAgentCard(card));
    }

    private AgentCard unmarshalFrom(String body) throws JsonProcessingException {
        org.a2aproject.sdk.grpc.AgentCard.Builder agentCardBuilder = org.a2aproject.sdk.grpc.AgentCard.newBuilder();
        JSONRPCUtils.parseJsonString(body, agentCardBuilder, "");
        return ProtoUtils.FromProto.agentCard(agentCardBuilder);
    }

    private String printAgentCard(AgentCard agentCard) throws InvalidProtocolBufferException {
        return JsonFormat.printer().print(ProtoUtils.ToProto.agentCard(agentCard));
    }

    @Test
    public void testGetWellKnownAgentCardJsonDecodeError() throws Exception {
        TestHttpClient client = createTestClient();
        client.body = "X" + JsonMessages.AGENT_CARD;

        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").build();

        assertThrows(A2AClientJSONError.class, resolver::getWellKnownAgentCard);
    }

    @Test
    public void testGetWellKnownAgentCardRequestError() throws Exception {
        TestHttpClient client = createTestClient();
        client.status = 503;

        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").build();

        A2AClientError error = assertThrows(A2AClientError.class, resolver::getWellKnownAgentCard);
        assertTrue(error.getMessage().contains("503"));
    }

    @Test
    public void testGetConfiguredAgentCard_fullUrlAlreadyContainingAgentCardPath() throws Exception {
        String fullUrl = "https://agentbin.greensmoke-1163cb63.eastus.azurecontainerapps.io/spec03/.well-known/agent-card.json";
        TestHttpClient client = createTestClient();

        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl(fullUrl).build();
        resolver.getConfiguredAgentCard();
        assertEquals(fullUrl, client.url);

        resolver.getWellKnownAgentCard();
        assertEquals(fullUrl, client.url);
    }

    @Test
    public void testGetWellKnownAgentCard_withTenant() throws Exception {
        TestHttpClient client = createTestClient();

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").tenant("my-tenant").build().getWellKnownAgentCard();

        assertEquals("http://example.com/my-tenant" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testGetConfiguredAgentCard_withCustomAgentCardPath() throws Exception {
        TestHttpClient client = createTestClient();

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").agentCardPath("/custom/agent.json").build().getConfiguredAgentCard();
        assertEquals("http://example.com/custom/agent.json", client.url);

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").agentCardPath("custom/agent.json").build().getConfiguredAgentCard();
        assertEquals("http://example.com/custom/agent.json", client.url);
    }

    @Test
    public void testGetWellKnownAgentCard_withAuthHeaders() throws Exception {
        TestHttpClient client = createTestClient();
        Map<String, String> authHeaders = Map.of("Authorization", "Bearer token123");

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").authHeaders(authHeaders).build().getWellKnownAgentCard();

        assertEquals("Bearer token123", client.capturedHeaders.get("Authorization"));
    }

    @Test
    public void testGetWellKnownAgentCard_ioExceptionThrowsA2AClientError() throws Exception {
        TestHttpClient client = createTestClient();
        client.throwIOException = true;

        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").build();

        assertThrows(A2AClientError.class, resolver::getWellKnownAgentCard);
    }

    @Test
    public void testGetWellKnownAgentCard_interruptedExceptionThrowsA2AClientError() throws Exception {
        TestHttpClient client = createTestClient();
        client.throwInterruptedException = true;

        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").build();

        assertThrows(A2AClientError.class, resolver::getWellKnownAgentCard);
    }

    @Test
    public void testBuilder_nullBaseUrl_throws() {
        assertThrows(IllegalArgumentException.class, () -> A2ACardResolver.builder().build());
    }

    @Test
    public void testSpec03PathPreservation() throws Exception {
        TestHttpClient client = createTestClient();

        A2ACardResolver.builder().httpClient(client).baseUrl("https://example.com/spec03").build().getWellKnownAgentCard();
        assertEquals("https://example.com/spec03" + AGENT_CARD_PATH, client.url);

        A2ACardResolver.builder().httpClient(client).baseUrl("https://example.com/spec03").tenant("my-tenant").build().getWellKnownAgentCard();
        assertEquals("https://example.com/spec03/my-tenant" + AGENT_CARD_PATH, client.url);

        A2ACardResolver.builder().httpClient(client).baseUrl("https://example.com/spec03").agentCardPath("/custom/card.json").build().getConfiguredAgentCard();
        assertEquals("https://example.com/spec03/custom/card.json", client.url);
    }

    @Test
    public void testBuilder_withAuthHeader() throws Exception {
        TestHttpClient client = createTestClient();

        A2ACardResolver.builder()
                .httpClient(client)
                .baseUrl("http://example.com")
                .authHeader("Authorization", "Bearer token123")
                .build()
                .getWellKnownAgentCard();

        assertEquals("Bearer token123", client.capturedHeaders.get("Authorization"));
    }

    private static class TestHttpClient implements A2AHttpClient {
        int status = 200;
        String body;
        String url;
        boolean throwIOException = false;
        boolean throwInterruptedException = false;
        Map<String, String> capturedHeaders = new HashMap<>();

        @Override
        public GetBuilder createGet() {
            return new TestGetBuilder();
        }

        @Override
        public PostBuilder createPost() {
            return null;
        }

        @Override
        public DeleteBuilder createDelete() {
            return null;
        }

        class TestGetBuilder implements A2AHttpClient.GetBuilder {

            @Override
            public A2AHttpResponse get() throws IOException, InterruptedException {
                if (throwIOException) {
                    throw new IOException("Simulated IO error");
                }
                if (throwInterruptedException) {
                    throw new InterruptedException("Simulated interrupt");
                }
                return new A2AHttpResponse() {
                    @Override
                    public int status() {
                        return status;
                    }

                    @Override
                    public boolean success() {
                        return status == 200;
                    }

                    @Override
                    public String body() {
                        return body;
                    }
                };
            }

            @Override
            public CompletableFuture<Void> getAsyncSSE(Consumer<ServerSentEvent> messageConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
                return null;
            }

            @Override
            public GetBuilder url(String s) {
                url = s;
                return this;
            }

            @Override
            public GetBuilder addHeader(String name, String value) {
                capturedHeaders.put(name, value);
                return this;
            }

            @Override
            public GetBuilder addHeaders(Map<String, String> headers) {
                capturedHeaders.putAll(headers);
                return this;
            }
        }
    }
}
