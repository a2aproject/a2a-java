package org.a2aproject.sdk.compat03.transport.grpc.handler;

import org.a2aproject.sdk.server.ServerCallContext;
import io.grpc.stub.StreamObserver;

/**
 * Factory interface for creating ServerCallContext from gRPC StreamObserver.
 * Implementations can provide custom context creation logic.
 */
public interface CallContextFactory {
    <V> ServerCallContext create(StreamObserver<V> responseObserver);
}
