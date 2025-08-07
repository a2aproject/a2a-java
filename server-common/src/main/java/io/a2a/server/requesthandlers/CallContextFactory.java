package io.a2a.server.requesthandlers;

import io.a2a.server.ServerCallContext;
import io.grpc.stub.StreamObserver;

public interface CallContextFactory {
    <V> ServerCallContext create(StreamObserver<V> responseObserver);
}
