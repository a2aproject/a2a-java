package org.a2aproject.sdk.server.common.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import java.util.Objects;
import java.util.concurrent.Flow;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Multi;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.events.EventConsumer;

/**
 * Utility for writing SSE (Server-Sent Events) responses over Vert.x HTTP.
 *
 * <p>Events are requested all upfront ({@code request(Long.MAX_VALUE)}) so that
 * back-to-back emissions from the EventConsumer are never dropped by a stalled
 * single-item demand window. This means the {@link EventConsumer}'s internal buffer
 * (256 items) acts as the only bound — write-level backpressure is not applied.
 * Ordering between the final {@code response.write()} and {@code response.end()} is
 * preserved by {@code EventConsumer.BUFFER_FLUSH_DELAY_MS}: the EventConsumer waits
 * briefly after sending the final event before calling {@code tube.complete()}, which
 * ensures every write callback has confirmed delivery before {@code onComplete} is
 * delivered to this subscriber.
 */
public final class SseResponseWriter {

    private static final Logger logger = LoggerFactory.getLogger(SseResponseWriter.class);
    private static final String SERVER_SENT_EVENTS = "text/event-stream";

    private SseResponseWriter() {
        // Utility class — no instances.
    }

    /**
     * Subscribes to {@code sseStrings} and writes each SSE event to the HTTP response.
     *
     * <p><b>Error handling:</b>
     * <ul>
     *   <li>Client disconnect → cancels upstream, stops polling</li>
     *   <li>Write failure → cancels upstream, fails routing context</li>
     *   <li>Stream error → cancels upstream, fails routing context</li>
     * </ul>
     *
     * @param sseStrings the SSE-formatted event stream
     * @param rc the Vert.x routing context
     * @param context the A2A server call context (for EventConsumer cancellation)
     * @param onSubscribedHook optional hook invoked once the subscriber is attached; used by tests
     */
    public static void writeSseStrings(
            Multi<String> sseStrings,
            RoutingContext rc,
            ServerCallContext context,
            @Nullable Runnable onSubscribedHook) {
        HttpServerResponse response = rc.response();

        sseStrings.subscribe().withSubscriber(new Flow.Subscriber<String>() {
            // Written in onSubscribe (EventConsumer / subscription thread), read inside
            // the write-failure callback (event loop thread) — volatile for visibility.
            volatile Flow.@Nullable Subscription upstream;
            // onNext and onComplete both run on the same EventConsumer polling thread,
            // so no volatile needed for headersSet.
            boolean headersSet = false;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.upstream = subscription;
                // Request all events upfront: the EventConsumer's BUFFER_FLUSH_DELAY_MS
                // sleep between tube.send(finalEvent) and tube.complete() guarantees that
                // every write callback confirms delivery before onComplete fires, so
                // response.end() is always called after the data is in flight.
                this.upstream.request(Long.MAX_VALUE);

                response.closeHandler(v -> {
                    logger.info("SSE connection closed by client, calling EventConsumer.cancel() to stop polling loop");
                    context.invokeEventConsumerCancelCallback();
                    subscription.cancel();
                });

                if (onSubscribedHook != null) {
                    onSubscribedHook.run();
                }
            }

            @Override
            public void onNext(String sseEvent) {
                Buffer data;
                if (!headersSet) {
                    headersSet = true;
                    MultiMap headers = response.headers();
                    if (headers.get(CONTENT_TYPE) == null) {
                        headers.set(CONTENT_TYPE, SERVER_SENT_EVENTS);
                    }
                    headers.set("Cache-Control", "no-cache");
                    headers.set("X-Accel-Buffering", "no");  // disables nginx proxy buffering
                    response.setChunked(true);
                    response.setWriteQueueMaxSize(1);  // Vert.x default buffering breaks SSE flushing

                    // Merge kickstart comment into first event to avoid an orphaned async write
                    // that could race with the error callback of the data write.
                    data = Buffer.buffer(": SSE stream started\n\n").appendBuffer(Buffer.buffer(sseEvent));
                } else {
                    data = Buffer.buffer(sseEvent);
                }

                response.write(data, ar -> {
                    if (ar.failed() && !rc.failed()) {
                        // NullAway: upstream is guaranteed non-null after onSubscribe
                        Objects.requireNonNull(upstream).cancel();
                        rc.fail(ar.cause());
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                // NullAway: upstream is guaranteed non-null after onSubscribe
                Objects.requireNonNull(upstream).cancel();
                if (!rc.failed()) {
                    rc.fail(throwable);
                }
            }

            @Override
            public void onComplete() {
                if (!headersSet) {
                    MultiMap headers = response.headers();
                    if (headers.get(CONTENT_TYPE) == null) {
                        headers.set(CONTENT_TYPE, SERVER_SENT_EVENTS);
                    }
                }
                // Guard against duplicate end() if the client disconnected concurrently
                if (!response.ended()) {
                    response.end();
                }
            }
        });
    }
}
