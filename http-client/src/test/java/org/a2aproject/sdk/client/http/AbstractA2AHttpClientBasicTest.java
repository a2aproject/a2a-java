package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public abstract class AbstractA2AHttpClientBasicTest {

    protected abstract A2AHttpClient createClient();

    @Test
    public void testCreateGet() {
        A2AHttpClient client = createClient();
        A2AHttpClient.GetBuilder builder = client.createGet();
        assertNotNull(builder);
    }

    @Test
    public void testCreatePost() {
        A2AHttpClient client = createClient();
        A2AHttpClient.PostBuilder builder = client.createPost();
        assertNotNull(builder);
    }

    @Test
    public void testCreateDelete() {
        A2AHttpClient client = createClient();
        A2AHttpClient.DeleteBuilder builder = client.createDelete();
        assertNotNull(builder);
    }

    @Test
    public void testBuilderUrlSetting() {
        A2AHttpClient client = createClient();
        A2AHttpClient.GetBuilder builder = client.createGet();
        A2AHttpClient.GetBuilder result = builder.url("https://example.com");
        assertSame(builder, result, "Builder should return itself for method chaining");
    }

    @Test
    public void testBuilderHeaderSetting() {
        A2AHttpClient client = createClient();
        A2AHttpClient.GetBuilder builder = client.createGet();
        A2AHttpClient.GetBuilder result = builder.addHeader("Accept", "application/json");
        assertSame(builder, result, "Builder should return itself for method chaining");
    }

    @Test
    public void testPostBuilderBody() {
        A2AHttpClient client = createClient();
        A2AHttpClient.PostBuilder builder = client.createPost();
        A2AHttpClient.PostBuilder result = builder.body("{\"key\":\"value\"}");
        assertSame(builder, result, "Builder should return itself for method chaining");
    }
}
