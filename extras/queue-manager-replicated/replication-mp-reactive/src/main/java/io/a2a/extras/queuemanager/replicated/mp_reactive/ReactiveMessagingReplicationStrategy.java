package io.a2a.extras.queuemanager.replicated.mp_reactive;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import io.a2a.extras.queuemanager.replicated.core.ReplicatedEventQueueItem;
import io.a2a.extras.queuemanager.replicated.core.ReplicationStrategy;
import io.a2a.json.JsonUtil;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ReactiveMessagingReplicationStrategy implements ReplicationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveMessagingReplicationStrategy.class);

    @Inject
    @Channel("replicated-events-out")
    private Emitter<String> emitter;

    @Inject
    private Event<ReplicatedEventQueueItem> cdiEvent;

    @Override
    public void send(String taskId, io.a2a.spec.Event event) {
        LOGGER.debug("Sending replicated event for task: {}, event: {}", taskId, event);

        try {
            ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, event);
            String json = JsonUtil.toJson(replicatedEvent);
            emitter.send(json);
            LOGGER.debug("Successfully sent replicated event for task: {}", taskId);
        } catch (Exception e) {
            LOGGER.error("Failed to send replicated event for task: {}, event: {}", taskId, event, e);
            throw new RuntimeException("Failed to send replicated event", e);
        }
    }

    @Incoming("replicated-events-in")
    public void onReplicatedEvent(String jsonMessage) {
        LOGGER.debug("Received replicated event JSON: {}", jsonMessage);

        try {
            ReplicatedEventQueueItem replicatedEvent = JsonUtil.fromJson(jsonMessage, ReplicatedEventQueueItem.class);
            LOGGER.debug("Deserialized replicated event for task: {}, event: {}",
                    replicatedEvent.getTaskId(), replicatedEvent.getEvent());

            // Fire the CDI event directly
            cdiEvent.fire(replicatedEvent);
            LOGGER.debug("Successfully fired CDI event for task: {}", replicatedEvent.getTaskId());
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize replicated event from JSON: {}", jsonMessage, e);
            // Don't throw - just log the error and continue processing other messages
            // This prevents one bad message from stopping the entire message processing
        }
    }
}