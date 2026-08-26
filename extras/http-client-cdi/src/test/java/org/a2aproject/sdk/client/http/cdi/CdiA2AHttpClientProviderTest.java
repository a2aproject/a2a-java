package org.a2aproject.sdk.client.http.cdi;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CdiA2AHttpClientProviderTest {

    @Nested
    class WithBeanInContainer {

        private SeContainer container;

        @BeforeEach
        void startContainer() {
            container = SeContainerInitializer.newInstance()
                    .disableDiscovery()
                    .addBeanClasses(DummyA2AHttpClient.class)
                    .initialize();
        }

        @AfterEach
        void closeContainer() {
            container.close();
        }

        @Test
        public void testCreateReturnsBeanWhenResolvable() {
            A2AHttpClient result = new CdiA2AHttpClientProvider().create();
            assertNotNull(result);
            assertInstanceOf(DummyA2AHttpClient.class, result);
        }
    }

    @Nested
    class WithoutBeanInContainer {

        private SeContainer container;

        @BeforeEach
        void startContainer() {
            container = SeContainerInitializer.newInstance()
                    .disableDiscovery()
                    .addBeanClasses(FillerBean.class)
                    .initialize();
        }

        @AfterEach
        void closeContainer() {
            container.close();
        }

        @Test
        public void testCreateThrowsWhenNoBeanRegistered() {
            assertThrows(IllegalStateException.class, new CdiA2AHttpClientProvider()::create);
        }
    }

    @Nested
    class WithAmbiguousBeansInContainer {

        private SeContainer container;

        @BeforeEach
        void startContainer() {
            container = SeContainerInitializer.newInstance()
                    .disableDiscovery()
                    .addBeanClasses(DummyA2AHttpClient.class, AnotherDummyA2AHttpClient.class)
                    .initialize();
        }

        @AfterEach
        void closeContainer() {
            container.close();
        }

        @Test
        public void testCreateThrowsWhenMultipleBeansRegistered() {
            assertThrows(IllegalStateException.class, new CdiA2AHttpClientProvider()::create);
        }
    }

    @Nested
    class WithoutCdiContainer {

        @Test
        public void testCreateThrowsWhenNoCdiContainer() {
            assertThrows(IllegalStateException.class, new CdiA2AHttpClientProvider()::create);
        }
    }
}
