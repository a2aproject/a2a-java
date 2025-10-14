package io.a2a.client.transport.grpc;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A simple {@link StreamObserver} adapter class that completes
 * a {@link CompletableFuture} when the observer is completed.
 * <p>
 * This observer uses the value passed to its {@link #onNext(Object)} method to complete
 * the {@link CompletableFuture}.
 * <p>
 * This observer should only be used in cases where a single result is expected. If more
 * that one call is made to {@link #onNext(Object)} then future will be completed with
 * an exception.
 *
 * @param <T> The type of objects received in this stream.
 */
public class SingleValueStreamObserver<T> implements StreamObserver<T> {

    private int count;

    private T result;

    private final CompletableFuture<T> resultFuture = new CompletableFuture<>();

    /**
     * Create a SingleValueStreamObserver.
     */
    public SingleValueStreamObserver() {
    }

    /**
     * Obtain the {@link CompletableFuture} that will be completed
     * when the {@link StreamObserver} completes.
     *
     * @return The CompletableFuture
     */
    public CompletionStage<T> completionStage() {
        return resultFuture;
    }

    @Override
    public void onNext(T value) {
        if (count++ == 0) {
            result = value;
        } else {
            resultFuture.completeExceptionally(new IllegalStateException("More than one result received."));
        }
    }

    @Override
    public void onError(Throwable t) {
        if (t instanceof StatusRuntimeException) {
            resultFuture.completeExceptionally(GrpcErrorMapper.mapGrpcError((StatusRuntimeException) t));
        } else {
            resultFuture.completeExceptionally(t);
        }
    }

    @Override
    public void onCompleted() {
        resultFuture.complete(result);
    }
}
