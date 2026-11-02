package org.a2aproject.sdk.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.auth.TaskOperation;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(JpaTaskAuthorizationCdiTest.Selected.class)
class JpaTaskAuthorizationCdiTest {
    public static class Selected implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(JpaTaskAuthorizationProvider.class);
        }
    }

    @Inject TaskAuthorizationProvider provider;
    @Inject JpaDatabaseTaskStore store;

    @Test
    void explicitlySelectedProviderIsUsedByContextAwareStore() {
        String id = UUID.randomUUID().toString();
        var context = JpaTaskOwnershipTest.context("cdi-owner");
        assertTrue(provider.getCapability(JpaTaskAuthorizationCapability.class).isPresent());
        provider.recordOwnership(context, id, TaskOperation.LIST_TASKS);
        store.save(
                Task.builder()
                        .id(id)
                        .contextId(id)
                        .status(
                                new TaskStatus(
                                        TaskState.TASK_STATE_WORKING,
                                        null,
                                        OffsetDateTime.parse("2026-01-01T00:00:00Z")))
                        .build(),
                false);
        var params = new ListTasksParams(id, null, 1, null, null, null, null, null);
        assertEquals(1, store.list(params, context).totalSize());
        assertEquals(id, store.list(params, context).tasks().get(0).id());
        assertEquals(
                0, store.list(params, JpaTaskOwnershipTest.context("different-owner")).totalSize());
    }
}
