package org.a2aproject.sdk.extras.taskstore.database.jpa;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real database startup deliberately fails when the engine is unavailable. */
public abstract class JpaTaskAuthorizationDatabaseResource implements QuarkusTestResourceLifecycleManager {
    private JdbcDatabaseContainer<?> database;

    protected abstract JdbcDatabaseContainer<?> createDatabase();

    @Override
    public Map<String, String> start() {
        database = createDatabase();
        database.start();
        Map<String, String> properties = new HashMap<>();
        properties.put("quarkus.datasource.\"a2a-java\".jdbc.url", database.getJdbcUrl());
        properties.put("quarkus.datasource.\"a2a-java\".username", database.getUsername());
        properties.put("quarkus.datasource.\"a2a-java\".password", database.getPassword());
        return properties;
    }

    @Override
    public void stop() {
        if (database != null) {
            database.stop();
        }
    }

    public static final class Postgres extends JpaTaskAuthorizationDatabaseResource {
        @Override
        protected JdbcDatabaseContainer<?> createDatabase() {
            return new PostgreSQLContainer("postgres:16");
        }
    }

    public static final class MySql extends JpaTaskAuthorizationDatabaseResource {
        @Override
        protected JdbcDatabaseContainer<?> createDatabase() {
            return new MySQLContainer("mysql:8.0")
                    .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");
        }
    }
}
