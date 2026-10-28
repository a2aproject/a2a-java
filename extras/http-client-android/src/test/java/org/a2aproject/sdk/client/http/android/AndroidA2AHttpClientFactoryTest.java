package org.a2aproject.sdk.client.http.android;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientConfiguration;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.http.JdkA2AHttpClient;
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
    public void testCreateExcludingAndroidFallsBackToJdkClient() {
        A2AHttpClientConfiguration configuration = A2AHttpClientConfiguration.builder()
                .excludedProviderNames(List.of("android"))
                .build();

        A2AHttpClient client = A2AHttpClientFactory.create(configuration);

        assertNotNull(client);
        assertInstanceOf(JdkA2AHttpClient.class, client,
                "Excluding Android should select the JDK fallback");
    }
}
