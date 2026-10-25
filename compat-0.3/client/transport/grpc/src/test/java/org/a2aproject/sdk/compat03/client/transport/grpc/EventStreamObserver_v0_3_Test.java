package org.a2aproject.sdk.compat03.client.transport.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.junit.jupiter.api.Test;

class EventStreamObserver_v0_3_Test {

    @Test
    void onCompleted_callsErrorHandlerWithNull() {
        List<StreamingEventKind_v0_3> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();

        Consumer<StreamingEventKind_v0_3> eventHandler = events::add;
        Consumer<Throwable> errorHandler = errors::add;
        EventStreamObserver_v0_3 observer = new EventStreamObserver_v0_3(eventHandler, errorHandler);

        observer.onCompleted();

        assertEquals(1, errors.size(), "errorHandler should have been called once");
        assertNull(errors.get(0), "errorHandler should be called with null to signal clean completion");
        assertEquals(0, events.size(), "eventHandler should not be called");
    }

    @Test
    void onCompleted_withNullErrorHandler_doesNotThrow() {
        Consumer<StreamingEventKind_v0_3> eventHandler = e -> {};
        EventStreamObserver_v0_3 observer = new EventStreamObserver_v0_3(eventHandler, null);

        observer.onCompleted();
    }
}
