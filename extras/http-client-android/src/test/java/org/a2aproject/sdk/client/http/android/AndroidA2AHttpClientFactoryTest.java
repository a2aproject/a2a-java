package org.a2aproject.sdk.client.http.android;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.junit.jupiter.api.Test;

public class AndroidA2AHttpClientFactoryTest {

    @Test
    public void testCreateReturnsAndroidClient() {
        // When both JDK and Android are on classpath, Android should be preferred due to higher priority (100)
        A2AHttpClient client = A2AHttpClientFactory.create();
        assertNotNull(client);
        assertInstanceOf(AndroidA2AHttpClient.class, client,
            "Factory should return AndroidA2AHttpClient when Android provider is available");
    }

    @Test
    public void testCreateWithAndroidProviderName() {
        A2AHttpClient client = A2AHttpClientFactory.create("android");
        assertNotNull(client);
        assertInstanceOf(AndroidA2AHttpClient.class, client,
            "Factory should return AndroidA2AHttpClient when 'android' provider is requested");
    }

    @Test
    public void testAndroidClientIsUsable() {
        A2AHttpClient client = A2AHttpClientFactory.create("android");
        assertNotNull(client);

        // Verify we can create builders
        A2AHttpClient.GetBuilder getBuilder = client.createGet();
        assertNotNull(getBuilder, "Should be able to create GET builder");

        A2AHttpClient.PostBuilder postBuilder = client.createPost();
        assertNotNull(postBuilder, "Should be able to create POST builder");

        A2AHttpClient.DeleteBuilder deleteBuilder = client.createDelete();
        assertNotNull(deleteBuilder, "Should be able to create DELETE builder");
    }
}
