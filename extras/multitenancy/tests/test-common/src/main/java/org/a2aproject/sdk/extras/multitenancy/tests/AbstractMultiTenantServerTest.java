package org.a2aproject.sdk.extras.multitenancy.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TextPart;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Transport-agnostic multitenancy server test driven through the real A2A {@link Client}.
 * Concrete {@code @QuarkusTest} subclasses implement the three transport hooks and each carry
 * exactly one transport's reference module on the classpath.
 */
public abstract class AbstractMultiTenantServerTest {

    /** Port every subclass's Quarkus test server and client URL/target must agree on. */
    protected static final int TEST_PORT = 8081;

    protected final int serverPort;
    private Client streamingClient;
    private Client nonStreamingClient;

    protected AbstractMultiTenantServerTest(int serverPort) {
        this.serverPort = serverPort;
    }

    /** e.g. "JSONRPC", "GRPC", "HTTP+JSON". */
    protected abstract String getTransportProtocol();

    /**
     * Base URL/target the client uses to reach the server. Must encode the same port passed to
     * this class's constructor (e.g. {@code "http://localhost:" + TEST_PORT}, or {@code "localhost:" + TEST_PORT}
     * for gRPC's schemeless target) — the two are not derived from each other.
     */
    protected abstract String getTransportUrl();

    /** Wire the transport-specific {@code Client} (channel/http-client factory). */
    protected abstract void configureTransport(ClientBuilder builder);

    // ---------- executor routing (all transports) ----------

    @Test
    public void knownTenantRoutesToAcmeExecutor() throws Exception {
        assertEquals(Tenants.ACME, sendAndGetArtifactText(Tenants.ACME, false));
    }

    @Test
    public void secondTenantRoutesToBetaExecutor() throws Exception {
        assertEquals(Tenants.BETA, sendAndGetArtifactText(Tenants.BETA, false));
    }

    @Test
    public void unknownTenantFallsBackToDefault() throws Exception {
        assertEquals(Tenants.DEFAULT_LABEL, sendAndGetArtifactText(Tenants.UNKNOWN, false));
    }

    @Test
    public void nullTenantUsesDefault() throws Exception {
        assertEquals(Tenants.DEFAULT_LABEL, sendAndGetArtifactText(null, false));
    }

    // ---------- streaming routing (all transports) ----------

    @Test
    public void streamingWithKnownTenant() throws Exception {
        assertEquals(Tenants.ACME, sendAndGetArtifactText(Tenants.ACME, true));
    }

    @Test
    public void streamingWithUnknownTenantUsesDefault() throws Exception {
        assertEquals(Tenants.DEFAULT_LABEL, sendAndGetArtifactText(Tenants.UNKNOWN, true));
    }

    // ---------- concurrency (all transports) ----------

    @Test
    public void concurrentRequestsForDifferentTenants() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            CompletableFuture<String> acme = CompletableFuture.supplyAsync(() -> sendQuietly(Tenants.ACME), pool);
            CompletableFuture<String> beta = CompletableFuture.supplyAsync(() -> sendQuietly(Tenants.BETA), pool);
            CompletableFuture<String> defaultFuture = CompletableFuture.supplyAsync(() -> sendQuietly(null), pool);
            assertEquals(Tenants.ACME, acme.get(30, TimeUnit.SECONDS));
            assertEquals(Tenants.BETA, beta.get(30, TimeUnit.SECONDS));
            assertEquals(Tenants.DEFAULT_LABEL, defaultFuture.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdown();
        }
    }

    // ---------- extended card via Client (all transports, incl. gRPC RPC) ----------

    @Test
    public void extendedCardKnownTenantResolvesToAcme() throws Exception {
        assertEquals("acme-extended", getNonStreamingClient().getExtendedAgentCard(Tenants.ACME, null).name());
    }

    @Test
    public void extendedCardSecondTenantResolvesToBeta() throws Exception {
        assertEquals("beta-extended", getNonStreamingClient().getExtendedAgentCard(Tenants.BETA, null).name());
    }

    @Test
    public void extendedCardUnknownTenantFallsBackToDefault() throws Exception {
        assertEquals("default-extended", getNonStreamingClient().getExtendedAgentCard(Tenants.UNKNOWN, null).name());
    }

    @Test
    public void extendedCardNullTenantUsesDefault() throws Exception {
        assertEquals("default-extended", getNonStreamingClient().getExtendedAgentCard((String) null, null).name());
    }

    // ---------- public well-known card (HTTP only; gRPC overrides to no-ops) ----------

    @Test
    public void publicCardWithoutTenantReturnsDefault() {
        assertEquals("Default Agent", getWellKnownCardName("/.well-known/agent-card.json"));
    }

    @Test
    public void publicCardWithAcmeTenant() {
        assertEquals("Acme Agent", getWellKnownCardName("/.well-known/acme/agent-card.json"));
    }

    @Test
    public void publicCardWithBetaTenant() {
        assertEquals("Beta Agent", getWellKnownCardName("/.well-known/beta/agent-card.json"));
    }

    @Test
    public void publicCardUnknownTenantReturns404() {
        RestAssured.given().when().get(wellKnownUrl("/.well-known/unknown-corp/agent-card.json"))
                .then().statusCode(404);
    }

    // ---------- lifecycle ----------

    @AfterEach
    public void closeClients() {
        if (streamingClient != null) {
            streamingClient.close();
        }
        if (nonStreamingClient != null) {
            nonStreamingClient.close();
        }
    }

    // ---------- helpers ----------

    private String sendQuietly(@Nullable String tenant) {
        try {
            return sendAndGetArtifactText(tenant, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String sendAndGetArtifactText(@Nullable String tenant, boolean streaming) throws Exception {
        Message message = Message.builder()
                .messageId(UUID.randomUUID().toString())
                .role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("hello")))
                .build();
        MessageSendParams.Builder params = MessageSendParams.builder().message(message);
        if (tenant != null) {
            params.tenant(tenant);
        }

        List<String> artifactTexts = new ArrayList<>();
        CountDownLatch done = new CountDownLatch(1);
        BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
            Task task = null;
            if (event instanceof TaskEvent te) {
                task = te.getTask();
            } else if (event instanceof TaskUpdateEvent tue) {
                task = tue.getTask();
            }
            if (task != null && task.artifacts() != null && !task.artifacts().isEmpty()) {
                Artifact artifact = task.artifacts().get(0);
                if (!artifact.parts().isEmpty() && artifact.parts().get(0) instanceof TextPart tp) {
                    artifactTexts.add(tp.text());
                }
            }
            if (task != null && task.status() != null && task.status().state().isFinal()) {
                done.countDown();
            }
        };

        Client client = streaming ? getStreamingClient() : getNonStreamingClient();
        client.sendMessage(params.build(), List.of(consumer), null, null);
        if (streaming) {
            // The streaming client returns as soon as the SSE connection is initiated; wait for
            // completion. The non-streaming client blocks in sendMessage() until the task is done,
            // so no latch is needed there.
            boolean completed = done.await(30, TimeUnit.SECONDS);
            assertTrue(completed, "Timed out waiting for streaming task to complete");
        }

        assertFalse(artifactTexts.isEmpty(), "Expected at least one artifact text event");
        return artifactTexts.get(artifactTexts.size() - 1);
    }

    private String getWellKnownCardName(String path) {
        String body = RestAssured.given().when().get(wellKnownUrl(path))
                .then().statusCode(200).extract().asString();
        String name = JsonPath.from(body).getString("name");
        assertNotNull(name, "Expected a card name in: " + body);
        return name;
    }

    private String wellKnownUrl(String path) {
        return "http://localhost:" + serverPort + path;
    }

    protected Client getStreamingClient() {
        if (streamingClient == null) {
            streamingClient = buildClient(true);
        }
        return streamingClient;
    }

    protected Client getNonStreamingClient() {
        if (nonStreamingClient == null) {
            nonStreamingClient = buildClient(false);
        }
        return nonStreamingClient;
    }

    private Client buildClient(boolean streaming) {
        AgentCard card = AgentCard.builder()
                .name("multitenant-client-card")
                .description("client-side card")
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder().streaming(true).extendedAgentCard(true).build())
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface(getTransportProtocol(), getTransportUrl())))
                .build();
        ClientBuilder builder = Client.builder(card)
                .clientConfig(new ClientConfig.Builder().setStreaming(streaming).build());
        configureTransport(builder);
        try {
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
