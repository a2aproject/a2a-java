package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class A2AHttpClientProviderTest {

    @Test
    public void testJdkProviderCreatesClient() {
        JdkA2AHttpClientProvider provider = new JdkA2AHttpClientProvider();
        A2AHttpClient client = provider.create();
        assertNotNull(client);
        assertInstanceOf(JdkA2AHttpClient.class, client);
    }

    @Test
    public void testJdkProviderCreatesClientWithSseParserConfig() {
        JdkA2AHttpClientProvider provider = new JdkA2AHttpClientProvider();
        SSEParserConfig config = SSEParserConfig.builder().maxBufferChars(4 * 1024 * 1024).build();
        A2AHttpClient client = provider.createWithSseConfig(config);
        assertNotNull(client);
        assertInstanceOf(JdkA2AHttpClient.class, client);
    }

    @Test
    public void testJdkProviderPriority() {
        JdkA2AHttpClientProvider provider = new JdkA2AHttpClientProvider();
        assertEquals(0, provider.priority(), "JDK provider should have priority 0");
    }

    @Test
    public void testJdkProviderName() {
        JdkA2AHttpClientProvider provider = new JdkA2AHttpClientProvider();
        assertEquals("jdk", provider.name(), "JDK provider name should be 'jdk'");
    }

    @Test
    public void testJdkProviderSupportsSseConfig() {
        JdkA2AHttpClientProvider provider = new JdkA2AHttpClientProvider();
        assertTrue(provider.supportsSseConfig(), "JDK provider must support SSE config");
    }

    @Test
    public void testDefaultProviderDoesNotSupportSseConfig() {
        A2AHttpClientProvider defaultProvider = new A2AHttpClientProvider() {
            @Override
            public A2AHttpClient create() {
                return new JdkA2AHttpClient();
            }

            @Override
            public String name() {
                return "test";
            }
        };
        assertFalse(defaultProvider.supportsSseConfig(),
                "Default interface implementation must return false");
    }
}
