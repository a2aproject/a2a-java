package org.a2aproject.sdk.compat03.server.rest.quarkus;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.android.AndroidA2AHttpClient;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfigBuilder;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerCompatibilityTest_v0_3;

@QuarkusTest
@TestProfile(CompatibilityTestProfile_v0_3.class)
public class QuarkusA2ARest_v0_3_CompatibilityAndroidTest extends AbstractA2AServerCompatibilityTest_v0_3 {
    public QuarkusA2ARest_v0_3_CompatibilityAndroidTest() { super(8081); }
    @Override protected String getTransportProtocol() { return "HTTP+JSON"; }
    @Override protected String getTransportUrl() { return "http://localhost:8081"; }
    @Override protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(RestTransport.class, new RestTransportConfigBuilder()
                .httpClient(new AndroidA2AHttpClient()));
    }
}
