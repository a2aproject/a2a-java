package org.a2aproject.sdk.extras.taskstore.database.jpa;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

@QuarkusTest
@TestProfile(JpaTaskAuthorizationDatabaseProfile.MySql.class)
@DisabledIfSystemProperty(named = "skipDockerTests", matches = "true")
public class JpaTaskAuthorizationMySqlTest extends JpaTaskAuthorizationDatabaseTest {
    @Override
    protected boolean mysql() {
        return true;
    }

    @Test
    void temporaryTextSchemaDemonstratesCaseAndAccentEqualityFailure() throws Exception {
        textColumnNegativeControl();
    }
}
