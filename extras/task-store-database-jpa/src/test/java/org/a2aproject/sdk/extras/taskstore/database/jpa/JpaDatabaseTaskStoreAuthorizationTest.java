package org.a2aproject.sdk.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.enterprise.inject.Instance;

import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.junit.jupiter.api.Test;

class JpaDatabaseTaskStoreAuthorizationTest {

    @Test
    @SuppressWarnings("unchecked")
    void listFailsClosedWhenContextIsNull() {
        TaskAuthorizationProvider authProvider = mock(TaskAuthorizationProvider.class);
        Instance<TaskAuthorizationProvider> instance = mock(Instance.class);
        when(instance.isResolvable()).thenReturn(true);
        when(instance.get()).thenReturn(authProvider);

        JpaDatabaseTaskStore store = new JpaDatabaseTaskStore(instance);

        ListTasksResult result = store.list(new ListTasksParams(), null);

        assertNotNull(result);
        assertEquals(0, result.tasks().size());
        assertEquals(0, result.totalSize());
    }
}
