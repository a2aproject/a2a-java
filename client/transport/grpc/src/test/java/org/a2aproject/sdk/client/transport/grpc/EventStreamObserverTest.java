package org.a2aproject.sdk.client.transport.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.junit.jupiter.api.Test;

class EventStreamObserverTest {

    @Test
    void onCompleted_callsErrorHandlerWithNull() {
        List<StreamingEventKind> events = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();

        Consumer<StreamingEventKind> eventHandler = events::add;
        Consumer<Throwable> errorHandler = errors::add;
        EventStreamObserver observer = new EventStreamObserver(eventHandler, errorHandler);

        observer.onCompleted();

        assertEquals(1, errors.size(), "errorHandler should have been called once");
        assertNull(errors.get(0), "errorHandler should be called with null to signal clean completion");
        assertEquals(0, events.size(), "eventHandler should not be called");
    }

    @Test
    void onCompleted_withNullErrorHandler_doesNotThrow() {
        Consumer<StreamingEventKind> eventHandler = e -> {};
        EventStreamObserver observer = new EventStreamObserver(eventHandler, null);

        observer.onCompleted();
    }
}
