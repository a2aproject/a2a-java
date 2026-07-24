package org.a2aproject.sdk.client.http.android;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.junit.jupiter.api.Test;

public class AndroidA2AHttpClientTest {

    @Test
    public void testNoArgsConstructor() {
        AndroidA2AHttpClient client = new AndroidA2AHttpClient();
        assertNotNull(client);
    }

    @Test
    public void testCreateGet() {
        AndroidA2AHttpClient client = new AndroidA2AHttpClient();
        A2AHttpClient.GetBuilder builder = client.createGet();
        assertNotNull(builder);
    }

    @Test
    public void testCreatePost() {
        AndroidA2AHttpClient client = new AndroidA2AHttpClient();
        A2AHttpClient.PostBuilder builder = client.createPost();
        assertNotNull(builder);
    }

    @Test
    public void testCreateDelete() {
        AndroidA2AHttpClient client = new AndroidA2AHttpClient();
        A2AHttpClient.DeleteBuilder builder = client.createDelete();
        assertNotNull(builder);
    }

    @Test
    public void testBuilderUrlSetting() {
        AndroidA2AHttpClient client = new AndroidA2AHttpClient();
        A2AHttpClient.GetBuilder builder = client.createGet();
        A2AHttpClient.GetBuilder result = builder.url("https://example.com");
        assertSame(builder, result, "Builder should return itself for method chaining");
    }

    @Test
    public void testBuilderHeaderSetting() {
        AndroidA2AHttpClient client = new AndroidA2AHttpClient();
        A2AHttpClient.GetBuilder builder = client.createGet();
        A2AHttpClient.GetBuilder result = builder.addHeader("Accept", "application/json");
        assertSame(builder, result, "Builder should return itself for method chaining");
    }

    @Test
    public void testPostBuilderBody() {
        AndroidA2AHttpClient client = new AndroidA2AHttpClient();
        A2AHttpClient.PostBuilder builder = client.createPost();
        A2AHttpClient.PostBuilder result = builder.body("{\"key\":\"value\"}");
        assertSame(builder, result, "Builder should return itself for method chaining");
    }
}
