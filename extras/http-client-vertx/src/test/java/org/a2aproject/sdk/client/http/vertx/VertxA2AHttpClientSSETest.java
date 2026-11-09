package org.a2aproject.sdk.client.http.vertx;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientSSETest;
import org.junit.jupiter.api.AfterEach;

public class VertxA2AHttpClientSSETest extends AbstractA2AHttpClientSSETest {

    private VertxA2AHttpClient vertxClient;

    @Override
    protected A2AHttpClient createClient() {
        vertxClient = new VertxA2AHttpClient();
        return vertxClient;
    }

    @AfterEach
    public void closeVertxClient() {
        if (vertxClient != null) {
            vertxClient.close();
        }
    }

}
