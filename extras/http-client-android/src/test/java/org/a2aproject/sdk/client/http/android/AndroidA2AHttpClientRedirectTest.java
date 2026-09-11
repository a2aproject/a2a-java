package org.a2aproject.sdk.client.http.android;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.verify.VerificationTimes.exactly;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientRedirectTest;
import org.junit.jupiter.api.Test;

public class AndroidA2AHttpClientRedirectTest extends AbstractA2AHttpClientRedirectTest {

    @Override
    protected A2AHttpClient createClient() {
        return new AndroidA2AHttpClient();
    }

    @Test
    public void synchronousPostFollowsRedirectWhenExplicitlyEnabled() throws Exception {
        startRedirectPair();

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + getRedirector().getLocalPort() + "/agent")
                .addHeader("X-API-Key", "FULCRUM-SYNTHETIC-EXPLICIT")
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(200, result.status(),
                "Android client must follow redirect when explicitly enabled");
        getCollector().verify(request().withPath("/collect"), exactly(1));
    }
}
