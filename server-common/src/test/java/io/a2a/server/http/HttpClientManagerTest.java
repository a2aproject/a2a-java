package io.a2a.server.http;

import io.a2a.client.http.HttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpClientManagerTest {

    private final HttpClientManager clientManager = new HttpClientManager();

    @Test
    public void testThrowsIllegalArgument() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clientManager.getOrCreate(null)
        );
    }

    @Test
    public void testValidateCacheInstance() {
        HttpClient client1 = clientManager.getOrCreate("http://localhost:8000");
        HttpClient client2 = clientManager.getOrCreate("http://localhost:8000");
        HttpClient client3 = clientManager.getOrCreate("http://localhost:8001");
        HttpClient client4 = clientManager.getOrCreate("http://remote_agent:8001");

        Assertions.assertSame(client1, client2);
        Assertions.assertNotSame(client1, client3);
        Assertions.assertNotSame(client1, client4);
        Assertions.assertNotSame(client3, client4);
    }

    @Test
    public void testValidateCacheNoPort() {
        HttpClient client1 = clientManager.getOrCreate("https://localhost");
        HttpClient client2 = clientManager.getOrCreate("https://localhost:443");
        HttpClient client3 = clientManager.getOrCreate("http://localhost");
        HttpClient client4 = clientManager.getOrCreate("http://localhost:80");

        Assertions.assertSame(client1, client2);
        Assertions.assertNotSame(client1, client3);
        Assertions.assertSame(client3, client4);
        Assertions.assertNotSame(client2, client4);
    }

    @Test
    public void testThrowsInvalidUrl() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> clientManager.getOrCreate("this_is_invalid")
        );
    }
}
