package org.a2aproject.sdk.client.http.vertx;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientRedirectTest;

public class VertxA2AHttpClientRedirectTest extends AbstractA2AHttpClientRedirectTest {

    @Override
    protected A2AHttpClient createClient() {
        return new VertxA2AHttpClient();
    }
}
