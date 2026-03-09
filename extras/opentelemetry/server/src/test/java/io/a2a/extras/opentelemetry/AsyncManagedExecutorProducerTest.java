package io.a2a.extras.opentelemetry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.Executor;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncManagedExecutorProducerTest {

    @Mock
    private ManagedExecutor managedExecutor;

    private AsyncManagedExecutorProducer producer;

    @BeforeEach
    void setUp() {
        producer = new AsyncManagedExecutorProducer();
    }

    @Nested
    class InitializationTests {
        @Test
        void init_withValidManagedExecutor_logsSuccessfully() {
            producer.managedExecutor = managedExecutor;

            assertDoesNotThrow(() -> producer.init());
            assertNotNull(producer.managedExecutor);
        }

        @Test
        void init_withNullManagedExecutor_logsWarning() {
            producer.managedExecutor = null;

            // Should not throw, just log warning
            assertDoesNotThrow(() -> producer.init());
            assertNull(producer.managedExecutor);
        }
    }

    @Nested
    class ProduceTests {
        @Test
        void produce_withValidManagedExecutor_returnsExecutor() {
            producer.managedExecutor = managedExecutor;

            Executor result = producer.produce();

            assertNotNull(result);
            assertSame(managedExecutor, result);
        }

        @Test
        void produce_withNullManagedExecutor_throwsIllegalStateException() {
            producer.managedExecutor = null;

            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> producer.produce()
            );

            assertEquals(
                "ManagedExecutor not injected - ensure MicroProfile Context Propagation is available",
                exception.getMessage()
            );
        }

        @Test
        void produce_returnsSameInstanceOnMultipleCalls() {
            producer.managedExecutor = managedExecutor;

            Executor result1 = producer.produce();
            Executor result2 = producer.produce();

            assertSame(result1, result2);
            assertSame(managedExecutor, result1);
        }
    }

    @Nested
    class CDIIntegrationTests {
        @Test
        void producer_hasCorrectAnnotations() {
            // Verify class has @ApplicationScoped
            assertTrue(
                AsyncManagedExecutorProducer.class.isAnnotationPresent(
                    jakarta.enterprise.context.ApplicationScoped.class
                )
            );

            // Verify class has @Alternative
            assertTrue(
                AsyncManagedExecutorProducer.class.isAnnotationPresent(
                    jakarta.enterprise.inject.Alternative.class
                )
            );

            // Verify class has @Priority with value 20
            assertTrue(
                AsyncManagedExecutorProducer.class.isAnnotationPresent(
                    jakarta.annotation.Priority.class
                )
            );
            assertEquals(
                20,
                AsyncManagedExecutorProducer.class.getAnnotation(
                    jakarta.annotation.Priority.class
                ).value()
            );
        }

        @Test
        void produceMethod_hasCorrectAnnotations() throws NoSuchMethodException {
            var method = AsyncManagedExecutorProducer.class.getMethod("produce");

            // Verify method has @Produces
            assertTrue(
                method.isAnnotationPresent(jakarta.enterprise.inject.Produces.class)
            );

            // Verify method has @Internal
            assertTrue(
                method.isAnnotationPresent(io.a2a.server.util.async.Internal.class)
            );
        }

        @Test
        void initMethod_hasPostConstructAnnotation() throws NoSuchMethodException {
            var method = AsyncManagedExecutorProducer.class.getMethod("init");

            assertTrue(
                method.isAnnotationPresent(jakarta.annotation.PostConstruct.class)
            );
        }

        @Test
        void managedExecutorField_hasInjectAnnotation() throws NoSuchFieldException {
            var field = AsyncManagedExecutorProducer.class.getDeclaredField("managedExecutor");

            assertTrue(
                field.isAnnotationPresent(jakarta.inject.Inject.class)
            );
        }
    }

    @Nested
    class ExecutorBehaviorTests {
        @Test
        void producedExecutor_canExecuteRunnables() {
            producer.managedExecutor = managedExecutor;
            Runnable task = mock(Runnable.class);

            Executor executor = producer.produce();
            executor.execute(task);

            verify(managedExecutor).execute(task);
        }

        @Test
        void producedExecutor_delegatesToManagedExecutor() {
            producer.managedExecutor = managedExecutor;
            Runnable task1 = mock(Runnable.class);
            Runnable task2 = mock(Runnable.class);

            Executor executor = producer.produce();
            executor.execute(task1);
            executor.execute(task2);

            verify(managedExecutor).execute(task1);
            verify(managedExecutor).execute(task2);
            verifyNoMoreInteractions(managedExecutor);
        }
    }
}
