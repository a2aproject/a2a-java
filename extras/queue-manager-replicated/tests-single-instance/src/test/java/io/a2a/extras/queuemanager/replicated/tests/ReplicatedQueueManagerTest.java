package io.a2a.extras.queuemanager.replicated.tests;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.a2a.extras.queuemanager.replicated.core.ReplicatedQueueManager;
import io.a2a.server.events.QueueManager;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Basic test to verify the ReplicatedQueueManager is properly configured.
 * For full integration testing with Kafka replication, see KafkaReplicationIntegrationTest.
 */
@QuarkusTest
public class ReplicatedQueueManagerTest {

    @Inject
    QueueManager queueManager;

    @Test
    public void testReplicationSystemIsConfigured() {
        // Verify that we're using the ReplicatedQueueManager
        assertInstanceOf(ReplicatedQueueManager.class, queueManager);
    }
}