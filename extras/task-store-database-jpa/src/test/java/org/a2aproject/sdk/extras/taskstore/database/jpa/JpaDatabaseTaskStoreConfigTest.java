package org.a2aproject.sdk.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.server.tasks.TaskStoreException;
import org.junit.jupiter.api.Test;

class JpaDatabaseTaskStoreConfigTest {

    @Test
    void initConfigRejectsNonpositiveScanLimit() throws Exception {
        for (long maxScanned : new long[] {0, -1}) {
            JpaDatabaseTaskStore store = configuredStore(maxScanned, 100);

            TaskStoreException exception = assertThrows(TaskStoreException.class, store::initConfig);

            assertEquals("a2a.max-scanned-tasks must be greater than zero", exception.getMessage());
        }
    }

    @Test
    void initConfigRejectsNonpositiveAuthorizationScanBatchSize() throws Exception {
        for (int batchSize : new int[] {0, -1}) {
            JpaDatabaseTaskStore store = configuredStore(10_000, batchSize);

            TaskStoreException exception = assertThrows(TaskStoreException.class, store::initConfig);

            assertEquals("a2a.authorization-scan-batch-size must be greater than zero", exception.getMessage());
        }
    }

    @Test
    void initConfigAcceptsPositiveScanLimits() throws Exception {
        JpaDatabaseTaskStore store = configuredStore(1, 1);

        assertDoesNotThrow(store::initConfig);
    }

    private static JpaDatabaseTaskStore configuredStore(long maxScanned, int batchSize) throws Exception {
        A2AConfigProvider configProvider = mock(A2AConfigProvider.class);
        when(configProvider.getValue("a2a.replication.grace-period-seconds")).thenReturn("15");
        when(configProvider.getValue("a2a.max-scanned-tasks")).thenReturn(Long.toString(maxScanned));
        when(configProvider.getValue("a2a.authorization-scan-batch-size")).thenReturn(Integer.toString(batchSize));
        JpaDatabaseTaskStore store = new JpaDatabaseTaskStore();
        Field field = JpaDatabaseTaskStore.class.getDeclaredField("configProvider");
        field.setAccessible(true);
        field.set(store, configProvider);
        return store;
    }
}
