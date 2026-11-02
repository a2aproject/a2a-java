package org.a2aproject.sdk.extras.taskstore.database.jpa;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

@QuarkusTest
@TestProfile(JpaTaskAuthorizationDatabaseProfile.Postgres.class)
@DisabledIfSystemProperty(named = "skipDockerTests", matches = "true")
public class JpaTaskAuthorizationPostgresTest extends JpaTaskAuthorizationDatabaseTest {
    @Override
    protected boolean mysql() {
        return false;
    }
}
