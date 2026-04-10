package org.a2aproject.sdk.compat03.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientError_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientJSONError_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class A2ACardResolver_v0_3_Test {

    private static final String AGENT_CARD_PATH = "/.well-known/agent-card.json";

    @Test
    public void testConstructorStripsSlashes() throws Exception {
        TestHttpClient client = new TestHttpClient();
        client.body = JsonMessages_v0_3.AGENT_CARD;

        A2ACardResolver_v0_3 resolver = new A2ACardResolver_v0_3(client, "http://example.com/");
        AgentCard_v0_3 card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);


        resolver = new A2ACardResolver_v0_3(client, "http://example.com");
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        // baseUrl with trailing slash, agentCardParth with leading slash
        resolver = new A2ACardResolver_v0_3(client, "http://example.com/", AGENT_CARD_PATH);
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        // baseUrl without trailing slash, agentCardPath with leading slash
        resolver = new A2ACardResolver_v0_3(client, "http://example.com", AGENT_CARD_PATH);
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        // baseUrl with trailing slash, agentCardPath without leading slash
        resolver = new A2ACardResolver_v0_3(client, "http://example.com/", AGENT_CARD_PATH.substring(1));
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        // baseUrl without trailing slash, agentCardPath without leading slash
        resolver = new A2ACardResolver_v0_3(client, "http://example.com", AGENT_CARD_PATH.substring(1));
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);
    }


    @Test
    public void testGetAgentCardSuccess() throws Exception {
        TestHttpClient client = new TestHttpClient();
        client.body = JsonMessages_v0_3.AGENT_CARD;

        A2ACardResolver_v0_3 resolver = new A2ACardResolver_v0_3(client, "http://example.com/");
        AgentCard_v0_3 card = resolver.getAgentCard();

        AgentCard_v0_3 expectedCard = JsonUtil_v0_3.fromJson(JsonMessages_v0_3.AGENT_CARD, AgentCard_v0_3.class);
        String expected = JsonUtil_v0_3.toJson(expectedCard);

        String requestCardString = JsonUtil_v0_3.toJson(card);
        assertEquals(expected, requestCardString);
    }

    @Test
    public void testGetAgentCardJsonDecodeError() throws Exception {
        TestHttpClient client = new TestHttpClient();
        client.body = "X" + JsonMessages_v0_3.AGENT_CARD;

        A2ACardResolver_v0_3 resolver = new A2ACardResolver_v0_3(client, "http://example.com/");

        boolean success = false;
        try {
            AgentCard_v0_3 card = resolver.getAgentCard();
            success = true;
        } catch (A2AClientJSONError_v0_3 expected) {
        }
        assertFalse(success);
    }


    @Test
    public void testGetAgentCardRequestError() throws Exception {
        TestHttpClient client = new TestHttpClient();
        client.status = 503;

        A2ACardResolver_v0_3 resolver = new A2ACardResolver_v0_3(client, "http://example.com/");

        String msg = null;
        try {
            AgentCard_v0_3 card = resolver.getAgentCard();
        } catch (A2AClientError_v0_3 expected) {
            msg = expected.getMessage();
        }
        assertTrue(msg.contains("503"));
    }

    private static class TestHttpClient implements A2AHttpClient_v0_3 {
        int status = 200;
        String body;
        String url;

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

        class TestGetBuilder implements A2AHttpClient_v0_3.GetBuilder {

            @Override
            public A2AHttpResponse_v0_3 get() throws IOException, InterruptedException {
                return new A2AHttpResponse_v0_3() {
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
            public CompletableFuture<Void> getAsyncSSE(Consumer<String> messageConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
                return null;
            }

            @Override
            public GetBuilder url(String s) {
                url = s;
                return this;
            }

            @Override
            public GetBuilder addHeader(String name, String value) {
                return this;
            }

            @Override
            public GetBuilder addHeaders(Map<String, String> headers) {
                return this;
            }
        }
    }

}
