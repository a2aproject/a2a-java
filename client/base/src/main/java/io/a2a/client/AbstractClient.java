package io.a2a.client;

import io.a2a.spec.AgentCard;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.a2a.util.Assert.checkNotNullParam;

/**
 * Abstract class representing an A2A client. Provides a standard set
 * of methods for interacting with an A2A agent, regardless of the underlying
 * transport protocol. It supports sending messages, managing tasks, and
 * handling event streams.
 */
public abstract class AbstractClient {

    private final List<BiConsumer<ClientEvent, AgentCard>> consumers;
    private final @Nullable Consumer<Throwable> streamingErrorHandler;

    public AbstractClient(List<BiConsumer<ClientEvent, AgentCard>> consumers) {
        this(consumers, null);
    }

    public AbstractClient(@NonNull List<BiConsumer<ClientEvent, AgentCard>> consumers, @Nullable Consumer<Throwable> streamingErrorHandler) {
        checkNotNullParam("consumers", consumers);
        this.consumers = consumers;
        this.streamingErrorHandler = streamingErrorHandler;
    }

    /**
     * Process the event using all configured consumers.
     */
    void consume(ClientEvent clientEventOrMessage, AgentCard agentCard) {
        for (BiConsumer<ClientEvent, AgentCard> consumer : consumers) {
            consumer.accept(clientEventOrMessage, agentCard);
        }
    }

    /**
     * Get the error handler that should be used during streaming.
     *
     * @return the streaming error handler
     */
    public @Nullable Consumer<Throwable> getStreamingErrorHandler() {
        return streamingErrorHandler;
    }
}