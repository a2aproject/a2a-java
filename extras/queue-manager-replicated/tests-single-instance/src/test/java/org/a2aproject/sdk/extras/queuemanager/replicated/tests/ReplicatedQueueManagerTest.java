package org.a2aproject.sdk.extras.queuemanager.replicated.tests;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import jakarta.inject.Inject;

import org.a2aproject.sdk.extras.queuemanager.replicated.core.ReplicatedQueueManager;
import org.a2aproject.sdk.testutils.docker.RequiresDocker;
import org.a2aproject.sdk.server.events.QueueManager;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Basic test to verify the ReplicatedQueueManager is properly configured.
 * For full integration testing with Kafka replication, see KafkaReplicationIntegrationTest.
 */
@QuarkusTest
@RequiresDocker
public class ReplicatedQueueManagerTest {

    @Inject
    QueueManager queueManager;

    @Test
    public void testReplicationSystemIsConfigured() {
        // Verify that we're using the ReplicatedQueueManager
        assertInstanceOf(ReplicatedQueueManager.class, queueManager);
    }
}