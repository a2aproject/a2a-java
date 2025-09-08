package io.a2a.client.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.a2a.util.Utils.OBJECT_MAPPER;
import static io.a2a.util.Utils.unmarshalFrom;
import static org.junit.jupiter.api.Assertions.*;


import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientJSONError;
import io.a2a.spec.AgentCard;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class A2ACardResolverTest {

    private static final String AGENT_CARD_PATH = "/.well-known/agent-card.json";
    private static final TypeReference<AgentCard> AGENT_CARD_TYPE_REFERENCE = new TypeReference<>() {};

    private WireMockServer server;

    @BeforeEach
    public void setUp() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();

        configureFor("localhost", server.port());
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testConstructorStripsSlashes() throws Exception {
        HttpClient client = HttpClient.createHttpClient("http://localhost:" + server.port());

        givenThat(get(urlPathEqualTo(AGENT_CARD_PATH))
                .willReturn(okForContentType("application/json", JsonMessages.AGENT_CARD)));

        givenThat(get(urlPathEqualTo("/subpath" + AGENT_CARD_PATH))
                .willReturn(okForContentType("application/json", JsonMessages.AGENT_CARD)));

        A2ACardResolver resolver = new A2ACardResolver(client);
        AgentCard card = resolver.getAgentCard();

        assertNotNull(card);
        verify(getRequestedFor(urlEqualTo(AGENT_CARD_PATH))
                .withHeader("Content-Type", equalTo("application/json")));


        resolver = new A2ACardResolver(client, AGENT_CARD_PATH);
        card = resolver.getAgentCard();

        assertNotNull(card);
        verify(getRequestedFor(urlEqualTo(AGENT_CARD_PATH))
                .withHeader("Content-Type", equalTo("application/json")));


        resolver = new A2ACardResolver("http://localhost:" + server.port());
        card = resolver.getAgentCard();

        assertNotNull(card);
        verify(getRequestedFor(urlEqualTo(AGENT_CARD_PATH))
                .withHeader("Content-Type", equalTo("application/json")));

        resolver = new A2ACardResolver("http://localhost:" + server.port() + AGENT_CARD_PATH);
        card = resolver.getAgentCard();

        assertNotNull(card);
        verify(getRequestedFor(urlEqualTo(AGENT_CARD_PATH))
                .withHeader("Content-Type", equalTo("application/json")));

        // baseUrl with trailing slash
        resolver = new A2ACardResolver("http://localhost:" + server.port() + "/");
        card = resolver.getAgentCard();

        assertNotNull(card);
        verify(getRequestedFor(urlEqualTo(AGENT_CARD_PATH))
                .withHeader("Content-Type", equalTo("application/json")));

        // Sub-path
        // baseUrl with trailing slash
        resolver = new A2ACardResolver("http://localhost:" + server.port() + "/subpath");
        card = resolver.getAgentCard();

        assertNotNull(card);
        verify(getRequestedFor(urlEqualTo("/subpath" + AGENT_CARD_PATH))
                .withHeader("Content-Type", equalTo("application/json")));
    }


    @Test
    public void testGetAgentCardSuccess() throws Exception {
        HttpClient client = HttpClient.createHttpClient("http://localhost:" + server.port());

        givenThat(get(urlPathEqualTo(AGENT_CARD_PATH))
                .willReturn(okForContentType("application/json", JsonMessages.AGENT_CARD)));

        A2ACardResolver resolver = new A2ACardResolver(client);
        AgentCard card = resolver.getAgentCard();

        AgentCard expectedCard = unmarshalFrom(JsonMessages.AGENT_CARD, AGENT_CARD_TYPE_REFERENCE);
        String expected = OBJECT_MAPPER.writeValueAsString(expectedCard);

        String requestCardString = OBJECT_MAPPER.writeValueAsString(card);
        assertEquals(expected, requestCardString);

        verify(getRequestedFor(urlEqualTo(AGENT_CARD_PATH))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void testGetAgentCardJsonDecodeError() throws Exception {
        HttpClient client = HttpClient.createHttpClient("http://localhost:" + server.port());

        givenThat(get(urlPathEqualTo(AGENT_CARD_PATH))
                .willReturn(okForContentType("application/json", "X" + JsonMessages.AGENT_CARD)));

        A2ACardResolver resolver = new A2ACardResolver(client);

        boolean success = false;
        try {
            AgentCard card = resolver.getAgentCard();
            success = true;
        } catch (A2AClientJSONError expected) {
        }
        assertFalse(success);

        verify(getRequestedFor(urlEqualTo(AGENT_CARD_PATH))
                .withHeader("Content-Type", equalTo("application/json")));
    }


    @Test
    public void testGetAgentCardRequestError() throws Exception {
        HttpClient client = HttpClient.createHttpClient("http://localhost:" + server.port());

        givenThat(get(urlPathEqualTo(AGENT_CARD_PATH))
                .willReturn(status(503)));

        A2ACardResolver resolver = new A2ACardResolver(client);

        String msg = null;
        try {
            AgentCard card = resolver.getAgentCard();
        } catch (A2AClientError expected) {
            msg = expected.getMessage();
        }
        assertTrue(msg.contains("503"));

        verify(getRequestedFor(urlEqualTo(AGENT_CARD_PATH))
                .withHeader("Content-Type", equalTo("application/json")));
    }

}
