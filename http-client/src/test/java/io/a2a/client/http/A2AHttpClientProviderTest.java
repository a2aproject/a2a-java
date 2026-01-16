package io.a2a.client.http;

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
    public void testJdkProviderPriority() {
        JdkA2AHttpClientProvider provider = new JdkA2AHttpClientProvider();
        assertEquals(0, provider.priority(), "JDK provider should have priority 0");
    }

    @Test
    public void testJdkProviderName() {
        JdkA2AHttpClientProvider provider = new JdkA2AHttpClientProvider();
        assertEquals("jdk", provider.name(), "JDK provider name should be 'jdk'");
    }
}
