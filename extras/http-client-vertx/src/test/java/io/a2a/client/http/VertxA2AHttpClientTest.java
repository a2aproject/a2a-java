package io.a2a.client.http;

import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;

public class VertxA2AHttpClientTest {

    @Test
    public void testNoArgsConstructor() {
        VertxA2AHttpClient client = new VertxA2AHttpClient();
        assertNotNull(client);
        client.close();
    }

    @Test
    public void testVertxParameterConstructor() {
        Vertx vertx = Vertx.vertx();
        VertxA2AHttpClient client = new VertxA2AHttpClient(vertx);
        assertNotNull(client);
        client.close();
        vertx.close();
    }

    @Test
    public void testVertxParameterConstructorNullThrows() {
        assertThrows(NullPointerException.class, () -> {
            new VertxA2AHttpClient(null);
        });
    }

    @Test
    public void testCreateGet() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.GetBuilder builder = client.createGet();
            assertNotNull(builder);
        }
    }

    @Test
    public void testCreatePost() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.PostBuilder builder = client.createPost();
            assertNotNull(builder);
        }
    }

    @Test
    public void testCreateDelete() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.DeleteBuilder builder = client.createDelete();
            assertNotNull(builder);
        }
    }

    @Test
    public void testBuilderUrlSetting() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.GetBuilder builder = client.createGet();
            A2AHttpClient.GetBuilder result = builder.url("https://example.com");
            assertSame(builder, result, "Builder should return itself for method chaining");
        }
    }

    @Test
    public void testBuilderHeaderSetting() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.GetBuilder builder = client.createGet();
            A2AHttpClient.GetBuilder result = builder.addHeader("Accept", "application/json");
            assertSame(builder, result, "Builder should return itself for method chaining");
        }
    }

    @Test
    public void testBuilderMethodChaining() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.GetBuilder builder = client.createGet()
                    .url("https://example.com")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer token");
            assertNotNull(builder);
        }
    }

    @Test
    public void testPostBuilderBody() {
        try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
            A2AHttpClient.PostBuilder builder = client.createPost();
            A2AHttpClient.PostBuilder result = builder.body("{\"key\":\"value\"}");
            assertSame(builder, result, "Builder should return itself for method chaining");
        }
    }
}
