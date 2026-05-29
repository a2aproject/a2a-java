package org.a2aproject.sdk.server.common.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CdiPropagatingExecutorProducerTest {

    @Nested
    class CDIAnnotationTests {
        @Test
        void producer_hasCorrectAnnotations() {
            assertTrue(
                CdiPropagatingExecutorProducer.class.isAnnotationPresent(
                    jakarta.enterprise.context.ApplicationScoped.class
                )
            );

            assertTrue(
                CdiPropagatingExecutorProducer.class.isAnnotationPresent(
                    jakarta.enterprise.inject.Alternative.class
                )
            );

            assertTrue(
                CdiPropagatingExecutorProducer.class.isAnnotationPresent(
                    jakarta.annotation.Priority.class
                )
            );
            assertEquals(
                20,
                CdiPropagatingExecutorProducer.class.getAnnotation(
                    jakarta.annotation.Priority.class
                ).value()
            );
        }

        @Test
        void produceMethod_hasCorrectAnnotations() throws NoSuchMethodException {
            var method = CdiPropagatingExecutorProducer.class.getMethod("produce");

            assertTrue(
                method.isAnnotationPresent(jakarta.enterprise.inject.Produces.class)
            );

            assertTrue(
                method.isAnnotationPresent(org.a2aproject.sdk.server.util.async.Internal.class)
            );
        }

        @Test
        void initMethod_hasPostConstructAnnotation() throws NoSuchMethodException {
            var method = CdiPropagatingExecutorProducer.class.getMethod("init");

            assertTrue(
                method.isAnnotationPresent(jakarta.annotation.PostConstruct.class)
            );
        }

        @Test
        void closeMethod_hasPreDestroyAnnotation() throws NoSuchMethodException {
            var method = CdiPropagatingExecutorProducer.class.getMethod("close");

            assertTrue(
                method.isAnnotationPresent(jakarta.annotation.PreDestroy.class)
            );
        }
    }

    @Nested
    class ProduceTests {
        @Test
        void produce_withNullExecutor_throwsIllegalStateException() {
            CdiPropagatingExecutorProducer producer = new CdiPropagatingExecutorProducer();

            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                producer::produce
            );

            assertNotNull(exception.getMessage());
        }
    }
}
