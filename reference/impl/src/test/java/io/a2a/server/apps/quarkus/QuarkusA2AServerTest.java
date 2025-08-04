package io.a2a.server.apps.quarkus;

import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2AServerTest extends AbstractA2AServerTest {

    public QuarkusA2AServerTest() {
        super(8081);
    }
}
