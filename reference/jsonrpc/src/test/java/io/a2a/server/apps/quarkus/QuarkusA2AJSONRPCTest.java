package io.a2a.server.apps.quarkus;

import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2AJSONRPCTest extends AbstractA2AServerTest {

    public QuarkusA2AJSONRPCTest() {
        super(8081);
    }
}
