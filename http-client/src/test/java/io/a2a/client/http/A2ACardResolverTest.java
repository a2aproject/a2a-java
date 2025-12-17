package io.a2a.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.a2a.grpc.utils.JSONRPCUtils;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.json.JsonProcessingException;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class A2ACardResolverTest {

    private static final String AGENT_CARD_PATH = "/.well-known/agent-card.json";

    @Test
    public void testConstructorStripsSlashes() throws Exception {
        TestHttpClient client = new TestHttpClient();
        client.body = JsonMessages.AGENT_CARD;

        A2ACardResolver resolver = new A2ACardResolver(client, "http://example.com/", "");
        AgentCard card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);


        resolver = new A2ACardResolver(client, "http://example.com", "");
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        // baseUrl with trailing slash, agentCardParth with leading slash
        resolver = new A2ACardResolver(client, "http://example.com/", AGENT_CARD_PATH);
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        // baseUrl without trailing slash, agentCardPath with leading slash
        resolver = new A2ACardResolver(client, "http://example.com", AGENT_CARD_PATH);
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        // baseUrl with trailing slash, agentCardPath without leading slash
        resolver = new A2ACardResolver(client, "http://example.com/", AGENT_CARD_PATH.substring(1));
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);

        // baseUrl without trailing slash, agentCardPath without leading slash
        resolver = new A2ACardResolver(client, "http://example.com", AGENT_CARD_PATH.substring(1));
        card = resolver.getAgentCard();

        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);
    }


    @Test
    public void testGetAgentCardSuccess() throws Exception {
        TestHttpClient client = new TestHttpClient();
        client.body = JsonMessages.AGENT_CARD;

        A2ACardResolver resolver = new A2ACardResolver(client, "http://example.com/", "");
        AgentCard card = resolver.getAgentCard();

        AgentCard expectedCard = unmarshalFrom(JsonMessages.AGENT_CARD);
        String expected = printAgentCard(expectedCard);

        String requestCardString = printAgentCard(card);
        assertEquals(expected, requestCardString);
    }

    private AgentCard unmarshalFrom(String body) throws JsonProcessingException {
        io.a2a.grpc.AgentCard.Builder agentCardBuilder = io.a2a.grpc.AgentCard.newBuilder();
        JSONRPCUtils.parseJsonString(body, agentCardBuilder, null);
        return ProtoUtils.FromProto.agentCard(agentCardBuilder);
    }

    private String printAgentCard(AgentCard agentCard) throws InvalidProtocolBufferException {
        return JsonFormat.printer().print(ProtoUtils.ToProto.agentCard(agentCard));
    }

    @Test
    public void testGetAgentCardJsonDecodeError() throws Exception {
        TestHttpClient client = new TestHttpClient();
        client.body = "X" + JsonMessages.AGENT_CARD;

        A2ACardResolver resolver = new A2ACardResolver(client, "http://example.com/", "");

        boolean success = false;
        try {
            AgentCard card = resolver.getAgentCard();
            success = true;
        } catch (A2AClientJSONError expected) {
        }
        assertFalse(success);
    }


    @Test
    public void testGetAgentCardRequestError() throws Exception {
        TestHttpClient client = new TestHttpClient();
        client.status = 503;

        A2ACardResolver resolver = new A2ACardResolver(client, "http://example.com/", "");

        String msg = null;
        try {
            AgentCard card = resolver.getAgentCard();
        } catch (A2AClientError expected) {
            msg = expected.getMessage();
        }
        assertTrue(msg.contains("503"));
    }

    private static class TestHttpClient implements A2AHttpClient {
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

        class TestGetBuilder implements A2AHttpClient.GetBuilder {

            @Override
            public A2AHttpResponse get() throws IOException, InterruptedException {
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
