package org.a2aproject.sdk.client.http.android;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientIntegrationTest;

public class AndroidA2AHttpClientIntegrationTest extends AbstractA2AHttpClientIntegrationTest {

    @Override
    protected A2AHttpClient createClient() {
        return new AndroidA2AHttpClient();
    }
}
