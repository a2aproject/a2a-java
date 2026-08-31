package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.spec.A2AClientError;
import org.a2aproject.sdk.spec.A2AClientHTTPError;
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

    // -------------------------------------------------------------------------
    // Well-known URL construction
    // -------------------------------------------------------------------------

    @Test
    public void testWellKnownUrl_trailingSlashStripped() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").build().getAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testWellKnownUrl_noTrailingSlash() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").build().getAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testWellKnownUrl_subPathPreserved_withTrailingSlash() throws Exception {
        TestHttpClient client = createTestClient();
        // URI.resolve() would have silently dropped the sub-path; string concat must not.
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/jsonrpc/").build().getAgentCard();
        assertEquals("http://example.com/jsonrpc" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testWellKnownUrl_subPathPreserved_noTrailingSlash() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/jsonrpc").build().getAgentCard();
        assertEquals("http://example.com/jsonrpc" + AGENT_CARD_PATH, client.url);
    }

    // -------------------------------------------------------------------------
    // Custom agent card path URL construction
    // -------------------------------------------------------------------------

    @Test
    public void testCustomPath_withLeadingSlash() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").agentCardPath(AGENT_CARD_PATH).build().getAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testCustomPath_withoutLeadingSlash() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").agentCardPath(AGENT_CARD_PATH.substring(1)).build().getAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testCustomPath_baseUrlTrailingSlash_withLeadingSlash() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").agentCardPath(AGENT_CARD_PATH).build().getAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testCustomPath_baseUrlTrailingSlash_withoutLeadingSlash() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").agentCardPath(AGENT_CARD_PATH.substring(1)).build().getAgentCard();
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testCustomPath_doesNotIntroduceDoubleSlash() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").agentCardPath("/custom/agent.json").build().getAgentCard();
        assertEquals("http://example.com/custom/agent.json", client.url);
    }

    @Test
    public void testCustomPath_subBaseUrl() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/jsonrpc/").agentCardPath(AGENT_CARD_PATH).build().getAgentCard();
        assertEquals("http://example.com/jsonrpc" + AGENT_CARD_PATH, client.url);
    }

    // -------------------------------------------------------------------------
    // Fetch success / error
    // -------------------------------------------------------------------------

    @Test
    public void testGetAgentCard_success() throws Exception {
        TestHttpClient client = createTestClient();
        AgentCard card = A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").build().getAgentCard();
        assertEquals(printAgentCard(unmarshalFrom(JsonMessages.AGENT_CARD)), printAgentCard(card));
    }

    @Test
    public void testGetAgentCard_jsonDecodeError() throws Exception {
        TestHttpClient client = createTestClient();
        client.body = "X" + JsonMessages.AGENT_CARD;
        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").build();
        assertThrows(A2AClientJSONError.class, resolver::getAgentCard);
    }

    @Test
    public void testGetAgentCard_httpErrorThrows() throws Exception {
        TestHttpClient client = createTestClient();
        client.status = 503;
        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com/").build();
        A2AClientHTTPError error = assertThrows(A2AClientHTTPError.class, resolver::getAgentCard);
        assertTrue(error.getMessage().contains("503"));
        assertEquals(503, error.getCode());
    }

    @Test
    public void testGetAgentCard_customPath_httpErrorThrows_noFallback() throws Exception {
        TestHttpClient client = createTestClient();
        client.status = 404;
        A2ACardResolver resolver = A2ACardResolver.builder()
                .httpClient(client)
                .baseUrl("http://example.com")
                .agentCardPath("/custom/agent.json")
                .build();
        A2AClientHTTPError error = assertThrows(A2AClientHTTPError.class, resolver::getAgentCard);
        assertEquals(404, error.getCode());
        assertEquals(1, client.urlsCalled.size());
        assertEquals("http://example.com/custom/agent.json", client.urlsCalled.get(0));
    }

    @Test
    public void testGetAgentCard_ioExceptionThrows() throws Exception {
        TestHttpClient client = createTestClient();
        client.throwIOException = true;
        assertThrows(A2AClientError.class,
                A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").build()::getAgentCard);
    }

    @Test
    public void testGetAgentCard_interruptedExceptionThrows() throws Exception {
        TestHttpClient client = createTestClient();
        client.throwInterruptedException = true;
        assertThrows(A2AClientError.class,
                A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").build()::getAgentCard);
    }

    // -------------------------------------------------------------------------
    // Tenant, auth headers, builder validation
    // -------------------------------------------------------------------------

    @Test
    public void testGetAgentCard_withTenant() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").tenant("my-tenant").build().getAgentCard();
        assertEquals("http://example.com/.well-known/my-tenant/agent-card.json", client.url);
    }

    @Test
    public void testGetAgentCard_withTenantAndCustomAgentCardPath() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder()
                .httpClient(client)
                .baseUrl("http://example.com")
                .tenant("acme")
                .agentCardPath("/custom/card.json")
                .build()
                .getAgentCard();
        assertEquals("http://example.com/acme/custom/card.json", client.url);
    }

    @Test
    public void testGetAgentCard_withCustomPath_absoluteAndRelative() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").agentCardPath("/custom/agent.json").build().getAgentCard();
        assertEquals("http://example.com/custom/agent.json", client.url);

        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").agentCardPath("custom/agent.json").build().getAgentCard();
        assertEquals("http://example.com/custom/agent.json", client.url);
    }

    @Test
    public void testGetAgentCard_withAuthHeadersMap() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com")
                .authHeaders(Map.of("Authorization", "Bearer token123")).build().getAgentCard();
        assertEquals("Bearer token123", client.capturedHeaders.get("Authorization"));
    }

    @Test
    public void testGetAgentCard_withAuthHeaderSingle() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com")
                .authHeader("Authorization", "Bearer token123").build().getAgentCard();
        assertEquals("Bearer token123", client.capturedHeaders.get("Authorization"));
    }

    @Test
    public void testBuilder_nullBaseUrl_throws() {
        assertThrows(IllegalArgumentException.class, () -> A2ACardResolver.builder().build());
    }

    @Test
    public void testBuilder_malformedBaseUrl_throws() {
        assertThrows(A2AClientError.class, () -> A2ACardResolver.builder().baseUrl("not-a-url").build());
    }

    @Test
    public void testFullWellKnownUrlWithTenant() throws Exception {
        // Full well-known URL + tenant must strip the suffix before embedding tenant inside the path,
        // not produce a malformed path like ...agent-card.json/.well-known/my-tenant/agent-card.json
        TestHttpClient client = createTestClient();
        A2ACardResolver resolver = A2ACardResolver.builder()
                .httpClient(client)
                .baseUrl("https://example.com/.well-known/agent-card.json")
                .tenant("my-tenant")
                .build();
        resolver.getAgentCard();
        assertEquals("https://example.com/.well-known/my-tenant/agent-card.json", client.url);
    }

    // -------------------------------------------------------------------------
    // Spec03 / full-URL edge cases
    // -------------------------------------------------------------------------

    @Test
    public void testSpec03PathPreservation_wellKnown() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("https://example.com/spec03").build().getAgentCard();
        assertEquals("https://example.com/spec03" + AGENT_CARD_PATH, client.url);
    }

    @Test
    public void testSpec03PathPreservation_withTenant() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("https://example.com/spec03").tenant("my-tenant").build().getAgentCard();
        assertEquals("https://example.com/spec03/.well-known/my-tenant/agent-card.json", client.url);
    }

    @Test
    public void testSpec03PathPreservation_withCustomPath() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("https://example.com/spec03").agentCardPath("/custom/card.json").build().getAgentCard();
        assertEquals("https://example.com/spec03/custom/card.json", client.url);
    }

    @Test
    public void testBaseUrl_alreadyContainsWellKnownPath() throws Exception {
        String fullUrl = "https://agentbin.greensmoke-1163cb63.eastus.azurecontainerapps.io/spec03/.well-known/agent-card.json";
        TestHttpClient client = createTestClient();
        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl(fullUrl).build();
        resolver.getAgentCard();
        assertEquals(fullUrl, client.url);
    }

    @Test
    public void testFullWellKnownUrlWithCustomAgentCardPath() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver resolver = A2ACardResolver.builder()
                .httpClient(client)
                .baseUrl("https://example.com/spec03/.well-known/agent-card.json")
                .agentCardPath("/custom/card.json")
                .build();
        resolver.getAgentCard();
        assertEquals("https://example.com/spec03/custom/card.json", client.url);
    }

    @Test
    public void testFullWellKnownUrlWithSameAgentCardPath() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver resolver = A2ACardResolver.builder()
                .httpClient(client)
                .baseUrl("https://example.com/spec03/.well-known/agent-card.json")
                .agentCardPath("/.well-known/agent-card.json")
                .build();
        resolver.getAgentCard();
        assertEquals("https://example.com/spec03/.well-known/agent-card.json", client.url);
    }

    // -------------------------------------------------------------------------
    // Tenant URL — correct /.well-known/{tenant}/agent-card.json pattern
    // -------------------------------------------------------------------------

    @Test
    public void testTenantUrl_embeddedInWellKnownPath() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").tenant("acme").build().getAgentCard();
        assertEquals("http://example.com/.well-known/acme/agent-card.json", client.url);
    }

    @Test
    public void testTenantUrl_fullTenantCardUrlProvidedAsBase_notDuplicated() throws Exception {
        // When the caller already supplies the tenant card URL, it must be used as-is.
        TestHttpClient client = createTestClient();
        String tenantCardUrl = "http://example.com/.well-known/acme/agent-card.json";
        A2ACardResolver.builder().httpClient(client).baseUrl(tenantCardUrl).tenant("acme").build().getAgentCard();
        assertEquals(tenantCardUrl, client.url);
        assertEquals(1, client.urlsCalled.size());
    }

    @Test
    public void testTenantUrl_fullTenantCardUrlProvidedAsBase_withTrailingSlash_notDuplicated() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client)
                .baseUrl("http://example.com/.well-known/acme/agent-card.json/")
                .tenant("acme")
                .build()
                .getAgentCard();
        assertEquals("http://example.com/.well-known/acme/agent-card.json", client.url);
        assertEquals(1, client.urlsCalled.size());
    }

    @Test
    public void testTenantUrl_fullTenantCardUrlProvidedAsBase_noFallback() throws Exception {
        // No fallback when the provided URL already IS the computed card URL.
        TestHttpClient client = createTestClient();
        client.status = 404;
        String tenantCardUrl = "http://example.com/.well-known/acme/agent-card.json";
        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl(tenantCardUrl).tenant("acme").build();
        assertThrows(A2AClientHTTPError.class, resolver::getAgentCard);
        assertEquals(1, client.urlsCalled.size());
    }

    @Test
    public void testTenantUrl_withSubBasePath() throws Exception {
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("https://example.com/spec03").tenant("acme").build().getAgentCard();
        assertEquals("https://example.com/spec03/.well-known/acme/agent-card.json", client.url);
    }

    // -------------------------------------------------------------------------
    // Fallback to provided URL on HTTP error
    // -------------------------------------------------------------------------

    @Test
    public void testGetAgentCard_httpError_fallsBackToProvidedUrl() throws Exception {
        // Primary URL (/.well-known/agent-card.json) returns 404; fallback to original base URL succeeds.
        TestHttpClient client = createTestClient();
        client.statusSequence.add(404);
        client.statusSequence.add(200);
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").build().getAgentCard();
        assertEquals(2, client.urlsCalled.size());
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.urlsCalled.get(0));
        assertEquals("http://example.com", client.urlsCalled.get(1));
    }

    @Test
    public void testGetAgentCard_doubleSlashBaseUrl_fallbackUrlNormalized() throws Exception {
        // A baseUrl with a double trailing slash must not produce a double-slash fallback URL.
        TestHttpClient client = createTestClient();
        client.statusSequence.add(404);
        client.statusSequence.add(200);
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com//").build().getAgentCard();
        assertEquals(2, client.urlsCalled.size());
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.urlsCalled.get(0));
        // cleanBase strips one trailing slash from "http://example.com//", yielding "http://example.com/"
        assertEquals("http://example.com/", client.urlsCalled.get(1));
    }

    @Test
    public void testGetAgentCard_doubleSlashWellKnownUrl_normalizedCardUrl() throws Exception {
        // A baseUrl that is already the well-known URL but with a double slash before /.well-known
        // must strip the well-known suffix and rebuild cleanly.
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com//.well-known/agent-card.json").build().getAgentCard();
        assertEquals(1, client.urlsCalled.size());
        assertEquals("http://example.com" + AGENT_CARD_PATH, client.urlsCalled.get(0));
    }

    @Test
    public void testGetAgentCard_doubleSlashTenantCardUrl_sameTenant_normalizedCardUrl() throws Exception {
        // baseUrl is the tenant card URL with a double slash; tenant matches — must normalize cleanly.
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client)
                .baseUrl("http://example.com//.well-known/acme/agent-card.json")
                .tenant("acme")
                .build()
                .getAgentCard();
        assertEquals(1, client.urlsCalled.size());
        assertEquals("http://example.com/.well-known/acme/agent-card.json", client.urlsCalled.get(0));
    }

    @Test
    public void testGetAgentCard_doubleSlashTenantCardUrl_differentTenant_normalizedCardUrl() throws Exception {
        // baseUrl is a tenant card URL with a double slash; a different tenant is requested — the
        // suffix must be stripped before the new tenant path is embedded.
        TestHttpClient client = createTestClient();
        A2ACardResolver.builder().httpClient(client)
                .baseUrl("http://example.com//.well-known/acme/agent-card.json")
                .tenant("foo")
                .build()
                .getAgentCard();
        assertEquals(1, client.urlsCalled.size());
        assertEquals("http://example.com/.well-known/foo/agent-card.json", client.urlsCalled.get(0));
    }

    @Test
    public void testGetAgentCard_httpError_bothFail_throwsLastError() throws Exception {
        // Both primary (/.well-known/agent-card.json) and fallback return 404; last error is propagated
        // and the primary error is attached as a suppressed exception.
        TestHttpClient client = createTestClient();
        client.status = 404;
        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").build();
        A2AClientHTTPError error = assertThrows(A2AClientHTTPError.class, resolver::getAgentCard);
        assertEquals(404, error.getCode());
        assertEquals(2, client.urlsCalled.size());
        assertEquals(1, error.getSuppressed().length);
        assertTrue(error.getSuppressed()[0] instanceof A2AClientHTTPError);
        assertEquals(404, ((A2AClientHTTPError) error.getSuppressed()[0]).getCode());
    }


    @Test
    public void testGetAgentCard_nonNotFound_noFallback() throws Exception {
        // A 5xx from the primary URL is not a URL-format issue, so no fallback is attempted.
        TestHttpClient client = createTestClient();
        client.status = 503;
        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").build();
        A2AClientHTTPError error = assertThrows(A2AClientHTTPError.class, resolver::getAgentCard);
        assertEquals(503, error.getCode());
        assertEquals(1, client.urlsCalled.size());
    }

    @Test
    public void testGetAgentCard_noFallback_whenUrlAlreadyIsCardUrl() throws Exception {
        // When the provided URL already equals the computed card URL, no fallback.
        TestHttpClient client = createTestClient();
        client.status = 404;
        String fullCardUrl = "http://example.com" + AGENT_CARD_PATH;
        A2ACardResolver resolver = A2ACardResolver.builder().httpClient(client).baseUrl(fullCardUrl).build();
        assertThrows(A2AClientHTTPError.class, resolver::getAgentCard);
        assertEquals(1, client.urlsCalled.size());
    }

    @Test
    public void testGetAgentCard_withTenant_httpError_fallsBackToProvidedUrl() throws Exception {
        TestHttpClient client = createTestClient();
        client.statusSequence.add(404);
        client.statusSequence.add(200);
        A2ACardResolver.builder().httpClient(client).baseUrl("http://example.com").tenant("acme").build().getAgentCard();
        assertEquals(2, client.urlsCalled.size());
        assertEquals("http://example.com/.well-known/acme/agent-card.json", client.urlsCalled.get(0));
        assertEquals("http://example.com", client.urlsCalled.get(1));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AgentCard unmarshalFrom(String body) throws JsonProcessingException {
        org.a2aproject.sdk.grpc.AgentCard.Builder agentCardBuilder = org.a2aproject.sdk.grpc.AgentCard.newBuilder();
        JSONRPCUtils.parseJsonString(body, agentCardBuilder, "");
        return ProtoUtils.FromProto.agentCard(agentCardBuilder);
    }

    private String printAgentCard(AgentCard agentCard) throws InvalidProtocolBufferException {
        return JsonFormat.printer().print(ProtoUtils.ToProto.agentCard(agentCard));
    }

    private static class TestHttpClient implements A2AHttpClient {
        int status = 200;
        List<Integer> statusSequence = new ArrayList<>();
        String body;
        String url;
        boolean throwIOException = false;
        boolean throwInterruptedException = false;
        Map<String, String> capturedHeaders = new HashMap<>();
        List<String> urlsCalled = new ArrayList<>();

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
                urlsCalled.add(url);
                if (throwIOException) {
                    throw new IOException("Simulated IO error");
                }
                if (throwInterruptedException) {
                    throw new InterruptedException("Simulated interrupt");
                }
                int effectiveStatus = statusSequence.isEmpty() ? status : statusSequence.remove(0);
                return new A2AHttpResponse() {
                    @Override
                    public int status() {
                        return effectiveStatus;
                    }

                    @Override
                    public boolean success() {
                        return effectiveStatus == 200;
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
