package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.mapstruct.Mapper;

/**
 * Bidirectional polymorphic mapper for converting StreamingEventKind between A2A Protocol v0.3 and v1.0.
 * <p>
 * Handles conversion for all StreamingEventKind implementers:
 * <ul>
 *   <li>{@link Task}</li>
 *   <li>{@link Message}</li>
 *   <li>{@link TaskStatusUpdateEvent}</li>
 *   <li>{@link TaskArtifactUpdateEvent}</li>
 * </ul>
 * <p>
 * Uses instanceof dispatch to determine the concrete type and delegates to the appropriate mapper.
 * <p>
 * Note: The same types implement both {@link org.a2aproject.sdk.spec.EventKind} and
 * {@link StreamingEventKind}, so this mapper uses the same delegation logic as {@link EventKindMapper_v0_3}.
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {
    TaskMapper_v0_3.class,
    MessageMapper_v0_3.class,
    TaskStatusUpdateEventMapper_v0_3.class,
    TaskArtifactUpdateEventMapper_v0_3.class
})
public interface StreamingEventKindMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    StreamingEventKindMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(StreamingEventKindMapper_v0_3.class);

    /**
     * Converts v0.3 StreamingEventKind to v1.0 StreamingEventKind.
     * <p>
     * Uses instanceof dispatch to determine the concrete type and delegates to the appropriate mapper.
     *
     * @param v03 the v0.3 streaming event kind
     * @return the equivalent v1.0 streaming event kind
     * @throws InvalidRequestError if the streaming event kind type is unrecognized
     */
    default StreamingEventKind toV10(StreamingEventKind_v0_3 v03) {
        if (v03 == null) {
            return null;
        }

        if (v03 instanceof Task_v0_3 v03Task) {
            return TaskMapper_v0_3.INSTANCE.toV10(v03Task);
        } else if (v03 instanceof Message_v0_3 v03Message) {
            return MessageMapper_v0_3.INSTANCE.toV10(v03Message);
        } else if (v03 instanceof TaskStatusUpdateEvent_v0_3 v03StatusUpdate) {
            return TaskStatusUpdateEventMapper_v0_3.INSTANCE.toV10(v03StatusUpdate);
        } else if (v03 instanceof TaskArtifactUpdateEvent_v0_3 v03ArtifactUpdate) {
            return TaskArtifactUpdateEventMapper_v0_3.INSTANCE.toV10(v03ArtifactUpdate);
        }

        throw new InvalidRequestError(null, "Unrecognized StreamingEventKind type: " + v03.getClass().getName(), null);
    }

    /**
     * Converts v1.0 StreamingEventKind to v0.3 StreamingEventKind.
     * <p>
     * Uses instanceof dispatch to determine the concrete type and delegates to the appropriate mapper.
     *
     * @param v10 the v1.0 streaming event kind
     * @return the equivalent v0.3 streaming event kind
     * @throws InvalidRequestError if the streaming event kind type is unrecognized
     */
    default StreamingEventKind_v0_3 fromV10(StreamingEventKind v10) {
        if (v10 == null) {
            return null;
        }

        if (v10 instanceof Task v10Task) {
            return TaskMapper_v0_3.INSTANCE.fromV10(v10Task);
        } else if (v10 instanceof Message v10Message) {
            return MessageMapper_v0_3.INSTANCE.fromV10(v10Message);
        } else if (v10 instanceof TaskStatusUpdateEvent v10StatusUpdate) {
            return TaskStatusUpdateEventMapper_v0_3.INSTANCE.fromV10(v10StatusUpdate);
        } else if (v10 instanceof TaskArtifactUpdateEvent v10ArtifactUpdate) {
            return TaskArtifactUpdateEventMapper_v0_3.INSTANCE.fromV10(v10ArtifactUpdate);
        }

        throw new InvalidRequestError(null, "Unrecognized StreamingEventKind type: " + v10.getClass().getName(), null);
    }
}
