package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.a2a.spec.Task;
import io.a2a.util.Utils;
import org.junit.jupiter.api.Test;

public class InMemoryTaskStoreTest {
    private static final String TASK_JSON = """
            {
                "id": "task-abc",
                "contextId" : "session-xyz",
                "status": {"state": "submitted"},
                "kind": "task"
            }""";

    @Test
    public void testSaveAndGet() throws Exception {
        InMemoryTaskStore store = new InMemoryTaskStore();
        Task task = Utils.unmarshalFrom(TASK_JSON, Task.TYPE_REFERENCE);
        store.save(task);
        Task retrieved = store.get(task.getId());
        assertSame(task, retrieved);
    }

    @Test
    public void testGetNonExistent() throws Exception {
        InMemoryTaskStore store = new InMemoryTaskStore();
        Task retrieved = store.get("nonexistent");
        assertNull(retrieved);
    }

    @Test
    public void testDelete() throws Exception {
        InMemoryTaskStore store = new InMemoryTaskStore();
        Task task = Utils.unmarshalFrom(TASK_JSON, Task.TYPE_REFERENCE);
        store.save(task);
        store.delete(task.getId());
        Task retrieved = store.get(task.getId());
        assertNull(retrieved);
    }

    @Test
    public void testDeleteNonExistent() throws Exception {
        InMemoryTaskStore store = new InMemoryTaskStore();
        store.delete("non-existent");
    }
}
