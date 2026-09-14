package org.a2aproject.sdk.client.http.android;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.SSEParserConfig;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AndroidA2AHttpClientProvider}.
 *
 * <p>The surefire configuration for this module sets {@code java.runtime.name} to
 * {@code "Android Runtime"}, so the provider treats the environment as Android.
 */
public class AndroidA2AHttpClientProviderTest {

    @Test
    public void testCreateWithSseConfigReturnsAndroidClient() {
        AndroidA2AHttpClientProvider provider = new AndroidA2AHttpClientProvider();
        SSEParserConfig config = SSEParserConfig.builder().maxBufferChars(4 * 1024 * 1024).build();

        A2AHttpClient client = provider.createWithSseConfig(config);
        assertNotNull(client);
        assertInstanceOf(AndroidA2AHttpClient.class, client,
                "Provider should return AndroidA2AHttpClient when Android runtime is available");
    }

    @Test
    public void testSupportsSseConfig() {
        AndroidA2AHttpClientProvider provider = new AndroidA2AHttpClientProvider();
        assertTrue(provider.supportsSseConfig(), "Android provider must support SSE config");
    }

    @Test
    public void testProviderName() {
        AndroidA2AHttpClientProvider provider = new AndroidA2AHttpClientProvider();
        assertEquals("android", provider.name());
    }

    @Test
    public void testPriorityOnAndroid() {
        AndroidA2AHttpClientProvider provider = new AndroidA2AHttpClientProvider();
        assertEquals(110, provider.priority(),
                "Android provider should have priority 110 when Android runtime is available");
    }
}
