package org.a2aproject.sdk.extras.taskstore.database.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.test.junit.QuarkusTestProfile;

public abstract class JpaTaskAuthorizationDatabaseProfile implements QuarkusTestProfile {
    protected abstract String kind();

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> properties = new HashMap<>();
        properties.put("quarkus.hibernate-orm.persistence-xml.ignore", "true");
        properties.put("quarkus.datasource.\"a2a-java\".db-kind", kind());
        properties.put("quarkus.datasource.\"a2a-java\".devservices.enabled", "false");
        properties.put("quarkus.datasource.\"a2a-java\".jdbc.max-size", "12");
        properties.put("quarkus.hibernate-orm.\"a2a-java\".datasource", "a2a-java");
        properties.put("quarkus.hibernate-orm.\"a2a-java\".packages", JpaTask.class.getPackageName());
        properties.put("quarkus.hibernate-orm.\"a2a-java\".database.generation", "drop-and-create");
        properties.put("quarkus.hibernate-orm.log.sql", "false");
        properties.put("quarkus.hibernate-orm.\"a2a-java\".log.sql", "false");
        properties.put("a2a.authorization.required", "true");
        return properties;
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(JpaTaskAuthorizationProvider.class);
    }

    public static final class Postgres extends JpaTaskAuthorizationDatabaseProfile {
        @Override protected String kind() { return "postgresql"; }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(new TestResourceEntry(JpaTaskAuthorizationDatabaseResource.Postgres.class));
        }
    }

    public static final class MySql extends JpaTaskAuthorizationDatabaseProfile {
        @Override protected String kind() { return "mysql"; }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(new TestResourceEntry(JpaTaskAuthorizationDatabaseResource.MySql.class));
        }
    }
}
